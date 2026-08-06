package com.nhakhoaquangninh.telesales.data.repository

import android.util.Log
import android.webkit.MimeTypeMap
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.remote.ApiService
import com.nhakhoaquangninh.telesales.data.remote.RetrofitClient
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.MessageProvider
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.repository.CallRecordRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class CallRecordRepositoryImpl(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val tokenManager: TokenManager,
    private val messageProvider: MessageProvider
) : CallRecordRepository {

    override suspend fun uploadCallRecord(metadata: CallRecordMetadata): Resource<Boolean> {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            return Resource.Error(
                message = messageProvider.getTokenMissingMessage(),
                source = ErrorSource.SERVER,
                code = 401
            )
        }

        val file = File(metadata.filePath)
        val extension = file.extension.lowercase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
        Log.d("UploadAudio", "Preparing upload - File: ${file.name} | Extension: $extension | MimeType: $mimeType")
        val requestFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
        val bodyPart = MultipartBody.Part.createFormData("recording", file.name, requestFile)
        val textMediaType = "text/plain".toMediaTypeOrNull()

        val response = apiService.uploadCallRecord(
            apiKey = RetrofitClient.DEFAULT_API_KEY,
            authorization = "Bearer $token",
            recording = bodyPart,
            phoneNumberFrom = metadata.phoneNumberFrom?.toRequestBody(textMediaType),
            phoneNumberTo = metadata.phoneNumberTo?.toRequestBody(textMediaType),
            callType = metadata.callType?.toRequestBody(textMediaType),
            duration = metadata.durationSeconds.toString().toRequestBody(textMediaType),
            callAt = metadata.callAtFormatted?.toRequestBody(textMediaType)
        )

        val code = response.code()
        return if (response.isSuccessful && (code == 200 || code == 201)) {
            Resource.Success(data = true, message = messageProvider.getUploadSuccessMessage())
        } else {
            val errBody = response.errorBody()?.string()
            if (code == 401) {
                tokenManager.clearSession()
                val message = ApiErrorParser.getServerMessageOrDefault(
                    errBody,
                    messageProvider.getTokenExpiredMessage()
                )
                Resource.Error(
                    message = message,
                    source = ErrorSource.SERVER,
                    code = 401,
                    rawDetails = errBody
                )
            } else {
                val message =
                    ApiErrorParser.getServerMessageOrDefault(errBody, messageProvider.getUploadFailedMessage())
                Resource.Error(
                    message = message,
                    source = ErrorSource.SERVER,
                    code = code,
                    rawDetails = errBody
                )
            }
        }
    }
}
