package com.nhakhoaquangninh.telesales.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.nhakhoaquangninh.telesales.core.FileLogger
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.remote.ApiService
import com.nhakhoaquangninh.telesales.data.remote.RetrofitClient
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.MessageProvider
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.repository.CallRecordRepository
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.io.FileNotFoundException
import java.io.IOException

class CallRecordRepositoryImpl(
    context: Context,
    private val apiService: ApiService = RetrofitClient.apiService,
    private val tokenManager: TokenManager,
    private val messageProvider: MessageProvider
) : CallRecordRepository {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    override suspend fun uploadCallRecord(metadata: CallRecordMetadata): Resource<Boolean> {
        val attemptId = java.util.UUID.randomUUID().toString()
        val trace = "call_id=${metadata.callId} attempt_id=$attemptId"
        val started = android.os.SystemClock.elapsedRealtime()
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            FileLogger.logLocal(appContext, "UPLOAD_RESULT", "$trace outcome=not_sent reason=missing_token")
            FileLogger.logNonFatalError(
                context = appContext,
                tag = "AUTH_ERROR",
                message = "Token không tồn tại hoặc chưa đăng nhập. Không thể upload cuộc gọi.",
                customKeys = mapOf("reason" to "missing_token")
            )
            return Resource.Error(
                message = messageProvider.getTokenMissingMessage(),
                source = ErrorSource.SERVER,
                code = 401
            )
        }

        val textMediaType = "text/plain".toMediaTypeOrNull()
        val isAnsweredString = if (metadata.isAnswered) "true" else "false"
        val careTypeValue = metadata.careType ?: tokenManager.getSelectedCareTypeValue()

        var bodyPart: MultipartBody.Part? = null
        var fileDetails = "recording_attached=false"
        val recordingUri = metadata.recordingUri
        if (metadata.isAnswered && !recordingUri.isNullOrBlank()) {
            val payload = resolvePayload(recordingUri)
            if (payload != null) {
                fileDetails = "recording_attached=true size_bytes=${payload.sizeBytes} mime=${FileLogger.safeDiagnostic(payload.mimeType)}"
                val recordingBody = ContentUriRequestBody(
                    resolver = resolver,
                    uri = payload.uri,
                    mediaType = payload.mimeType.toMediaType(),
                    contentLength = payload.sizeBytes
                )
                bodyPart = MultipartBody.Part.createFormData(
                    "recording",
                    payload.displayName,
                    recordingBody
                )
            } else {
                FileLogger.logNonFatalError(
                    context = appContext,
                    tag = "FILE_ERROR",
                    message = "$trace reason=payload_unreadable uri=${FileLogger.maskUri(recordingUri)}",
                    customKeys = mapOf("call_id" to metadata.callId)
                )
            }
        }

        FileLogger.log(
            appContext,
            "UPLOAD_START",
            "$trace backend=${RetrofitClient.BASE_URL} $fileDetails care_type=$careTypeValue is_answered=$isAnsweredString from=${FileLogger.maskPhone(metadata.phoneNumberFrom)} to=${FileLogger.maskPhone(metadata.phoneNumberTo)} type=${metadata.callType.wireValue} duration_s=${metadata.durationSeconds} call_at=${metadata.callAtFormatted}"
        )
        android.util.Log.d(
            "API_LOG",
            "--> Gửi Multipart POST /call-records | care_type=$careTypeValue | is_answered=$isAnsweredString | from=${FileLogger.maskPhone(metadata.phoneNumberFrom)} | to=${FileLogger.maskPhone(metadata.phoneNumberTo)} | type=${metadata.callType.wireValue} | duration=${metadata.durationSeconds}s | call_at=${metadata.callAtFormatted} | file=${if (bodyPart != null) "Có" else "Không"}"
        )

        val latitude = metadata.latitude
        val longitude = metadata.longitude
        val hasLocation = latitude != null && longitude != null &&
            latitude.isFinite() && longitude.isFinite() &&
            latitude in -90.0..90.0 && longitude in -180.0..180.0
        val latitudePart = if (hasLocation) latitude.toString().toRequestBody(textMediaType) else null
        val longitudePart = if (hasLocation) longitude.toString().toRequestBody(textMediaType) else null
        val locationReason = when {
            hasLocation -> "attached"
            latitude == null && longitude == null -> "missing_coordinates"
            else -> "invalid_or_incomplete_coordinates"
        }
        FileLogger.logLocal(
            appContext,
            "API_LOG",
            "location_upload call_id=${metadata.callId} latitude=${latitude.takeIf { hasLocation }} longitude=${longitude.takeIf { hasLocation }} latitude_present=${latitudePart != null} longitude_present=${longitudePart != null} location_attached=${latitudePart != null && longitudePart != null} reason=$locationReason"
        )
        val response = try {
            apiService.uploadCallRecord(
                apiKey = RetrofitClient.DEFAULT_API_KEY,
                authorization = "Bearer $token",
                recording = bodyPart,
                phoneNumberFrom = metadata.phoneNumberFrom?.toRequestBody(textMediaType),
                phoneNumberTo = metadata.phoneNumberTo?.toRequestBody(textMediaType),
                callType = metadata.callType.wireValue.toRequestBody(textMediaType),
                duration = metadata.durationSeconds.toString().toRequestBody(textMediaType),
                callAt = metadata.callAtFormatted?.toRequestBody(textMediaType),
                isAnswered = isAnsweredString.toRequestBody(textMediaType),
                careType = careTypeValue?.toString()?.toRequestBody(textMediaType),
                latitude = latitudePart,
                longitude = longitudePart
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            FileLogger.logLocal(appContext, "UPLOAD_RESULT", "$trace outcome=cancelled elapsed_ms=${android.os.SystemClock.elapsedRealtime() - started}")
            throw cancelled
        } catch (ioe: IOException) {
            FileLogger.logLocal(appContext, "UPLOAD_RESULT",
                "$trace outcome=io_error exception=${ioe.javaClass.simpleName} cause=${ioe.cause?.javaClass?.simpleName ?: "none"} elapsed_ms=${android.os.SystemClock.elapsedRealtime() - started}")
            val isFileNotFound = ioe is FileNotFoundException || ioe.cause is FileNotFoundException
            if (isFileNotFound) {
                FileLogger.logNonFatalError(
                    context = appContext,
                    tag = "FILE_NOT_FOUND",
                    message = "Tệp ghi âm bị mất khi đang stream upload: ${ioe.message}",
                    customKeys = mapOf("error" to (ioe.message ?: ""))
                )
                return Resource.Error(
                    message = messageProvider.getRecordingMissingMessage(),
                    source = ErrorSource.APP_CLIENT
                )
            }
            FileLogger.logException(
                appContext,
                "NETWORK_ERROR",
                "Mất kết nối máy chủ khi upload (IOException): ${ioe.message}",
                ioe
            )
            return Resource.Error(
                message = messageProvider.getConnectionFailedMessage(),
                source = ErrorSource.NETWORK
            )
        }

        val code = response.code()
        // Only known diagnostic headers, never raw response content or authentication headers.
        FileLogger.logLocal(appContext, "UPLOAD_RESULT",
            "$trace http=$code elapsed_ms=${android.os.SystemClock.elapsedRealtime() - started} " +
                "server_request_id=${FileLogger.safeDiagnostic(response.headers()["X-Request-ID"] ?: response.headers()["X-Correlation-ID"])} " +
                "retry_after=${FileLogger.safeDiagnostic(response.headers()["Retry-After"])} " +
                "server_date=${FileLogger.safeDiagnostic(response.headers()["Date"])}")
        return if (response.isSuccessful && code in setOf(200, 201)) {
            response.body()?.close()
            android.util.Log.d("API_LOG", "Upload thành công - HTTP $code")
            FileLogger.log(
                appContext,
                "API_SUCCESS",
                "$trace Upload thành công (HTTP $code)"
            )
            Resource.Success(data = true, message = messageProvider.getUploadSuccessMessage())
        } else {
            val errorBody = response.errorBody()?.string()
            if (code == 422) {
                // Log field names only: server messages may echo phone numbers or credentials.
                val allowedFields = setOf("recording", "is_answered", "duration", "call_type",
                    "phone_number_from", "phone_number_to", "call_at", "care_type", "latitude", "longitude")
                val rejectedFields = runCatching {
                    val errors = org.json.JSONObject(errorBody.orEmpty()).optJSONObject("errors")
                    errors?.keys()?.asSequence()?.filter { it in allowedFields }?.toList().orEmpty()
                }.getOrDefault(emptyList())
                FileLogger.logLocal(appContext, "API_VALIDATION",
                    "$trace http_code=422 rejected_fields=${rejectedFields.joinToString(",").ifEmpty { "unknown" }} " +
                        "recording_attached=${bodyPart != null} is_answered=$isAnsweredString")
            }
            android.util.Log.d("API_LOG", "Upload thất bại - HTTP $code")
            FileLogger.logNonFatalError(
                context = appContext,
                tag = "API_FAILURE",
                message = "$trace Upload bị từ chối (HTTP $code)",
                customKeys = mapOf(
                    "http_code" to code,
                    "care_type" to (careTypeValue ?: -1)
                )
            )
            if (code == 401) {
                tokenManager.clearSession()
                Resource.Error(
                    message = ApiErrorParser.getServerMessageOrDefault(
                        errorBody,
                        messageProvider.getTokenExpiredMessage()
                    ),
                    source = ErrorSource.SERVER,
                    code = code,
                    rawDetails = errorBody
                )
            } else {
                Resource.Error(
                    message = ApiErrorParser.getServerMessageOrDefault(
                        errorBody,
                        messageProvider.getUploadFailedMessage()
                    ),
                    source = ErrorSource.SERVER,
                    code = code,
                    rawDetails = errorBody
                )
            }
        }
    }

    private fun resolvePayload(uriValue: String): RecordingPayload? {
        val uri = runCatching { uriValue.toUri() }.getOrNull() ?: return null
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) return null
        return try {
            val rawMimeType = resolver.getType(uri)
            var displayName = "recording"
            var sizeBytes = -1L
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) {
                        displayName = cursor.getString(nameIndex)
                            ?.substringAfterLast('/')
                            ?.takeIf(String::isNotBlank)
                            ?: displayName
                    }
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                        sizeBytes = cursor.getLong(sizeIndex)
                    }
                }
            }
            val mimeType = resolveAudioMimeType(rawMimeType, displayName) ?: return null
            if (sizeBytes !in 1..MAX_SIZE_BYTES) return null
            resolver.openAssetFileDescriptor(uri, "r")?.use { } ?: return null
            RecordingPayload(uri, displayName, mimeType, sizeBytes)
        } catch (_: SecurityException) {
            null
        } catch (_: FileNotFoundException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun resolveAudioMimeType(rawMimeType: String?, displayName: String): String? {
        if (rawMimeType != null && rawMimeType.startsWith("audio/", ignoreCase = true)) {
            return rawMimeType
        }
        val ext = displayName.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "m4a" -> "audio/mp4"
            "mp3" -> "audio/mpeg"
            "amr" -> "audio/amr"
            "3gp", "3gpp" -> "audio/3gpp"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            "opus" -> "audio/opus"
            else -> if (rawMimeType == "application/octet-stream" || rawMimeType == null) "audio/mp4" else null
        }
    }

    private data class RecordingPayload(
        val uri: Uri,
        val displayName: String,
        val mimeType: String,
        val sizeBytes: Long
    )

    private class ContentUriRequestBody(
        private val resolver: ContentResolver,
        private val uri: Uri,
        private val mediaType: MediaType,
        private val contentLength: Long
    ) : RequestBody() {
        override fun contentType(): MediaType = mediaType

        override fun contentLength(): Long = contentLength

        override fun writeTo(sink: BufferedSink) {
            val input = resolver.openInputStream(uri)
                ?: throw FileNotFoundException("Recording URI is not readable")
            input.source().use(sink::writeAll)
        }
    }

    private companion object {
        const val MAX_SIZE_BYTES = 50L * 1024L * 1024L
    }
}
