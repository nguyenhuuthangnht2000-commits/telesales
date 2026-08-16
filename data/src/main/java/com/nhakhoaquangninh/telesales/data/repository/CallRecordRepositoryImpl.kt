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
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
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

        var bodyPart: MultipartBody.Part? = null
        if (metadata.isAnswered) {
            val payload = metadata.recordingUri?.let { resolvePayload(it) }
                ?: run {
                    FileLogger.logNonFatalError(
                        context = appContext,
                        tag = "FILE_ERROR",
                        message = "Không thể đọc tệp ghi âm hợp lệ từ URI: ${metadata.recordingUri}",
                        customKeys = mapOf("recordingUri" to (metadata.recordingUri ?: "null"))
                    )
                    return Resource.Error(
                        message = "Không thể đọc tệp ghi âm hợp lệ",
                        source = ErrorSource.APP_CLIENT
                    )
                }
            val recordingBody = ContentUriRequestBody(
                resolver = resolver,
                uri = payload.uri,
                mediaType = payload.mimeType.toMediaType(),
                contentLength = payload.sizeBytes
            )
            bodyPart = MultipartBody.Part.createFormData("recording", payload.displayName, recordingBody)
        }

        FileLogger.log(
            appContext,
            "UPLOAD_START",
            "Bắt đầu gửi request POST /call-records | isAnswered=$isAnsweredString | Từ: ${metadata.phoneNumberFrom} | Tới: ${metadata.phoneNumberTo} | Loại: ${metadata.callType.wireValue} | Thời lượng: ${metadata.durationSeconds}s | Lúc: ${metadata.callAtFormatted} | File đính kèm: ${if (bodyPart != null) "Có" else "Không (null)"}"
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
                isAnswered = isAnsweredString.toRequestBody(textMediaType)
            )
        } catch (ioe: IOException) {
            FileLogger.logException(appContext, "NETWORK_ERROR", "Mất kết nối máy chủ khi upload (IOException): ${ioe.message}", ioe)
            return Resource.Error(
                message = "Không thể kết nối máy chủ",
                source = ErrorSource.NETWORK
            )
        }

        val code = response.code()
        return if (response.isSuccessful && code in setOf(200, 201)) {
            val responseBody = response.body()?.string()
            android.util.Log.d("API_LOG", "Upload File Success - Code: $code, Body: $responseBody")
            FileLogger.log(appContext, "API_SUCCESS", "Upload thành công (HTTP $code) | Server phản hồi: $responseBody")
            Resource.Success(data = true, message = messageProvider.getUploadSuccessMessage())
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.d("API_LOG", "Upload File Failed - Code: $code, Body: $errorBody")
            FileLogger.logNonFatalError(
                context = appContext,
                tag = "API_FAILURE",
                message = "Upload bị từ chối (HTTP $code) | Chi tiết lỗi Server: $errorBody",
                customKeys = mapOf(
                    "http_code" to code,
                    "server_error_body" to (errorBody ?: "")
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