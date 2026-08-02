package com.nhakhoaquangninh.telesales.data.repository

import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.remote.ApiService
import com.nhakhoaquangninh.telesales.data.remote.RetrofitClient
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
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
    private val tokenManager: TokenManager
) : CallRecordRepository {

    override suspend fun uploadCallRecord(metadata: CallRecordMetadata): Resource<Boolean> {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            return Resource.Error(
                message = "Chưa có Token xác thực. Vui lòng đăng nhập lại!",
                source = ErrorSource.SERVER,
                code = 401
            )
        }

        val file = File(metadata.filePath)
        val requestFile = file.asRequestBody("audio/*".toMediaTypeOrNull())
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
            Resource.Success(data = true, message = "Tải file ghi âm thành công!")
        } else {
            val errBody = response.errorBody()?.string()
            if (code == 401) {
                tokenManager.clearSession()
                val message = ApiErrorParser.getServerMessageOrDefault(
                    errBody,
                    "Phiên đăng nhập hết hạn hoặc Token không hợp lệ."
                )
                Resource.Error(
                    message = message,
                    source = ErrorSource.SERVER,
                    code = 401,
                    rawDetails = errBody
                )
            } else {
                val message =
                    ApiErrorParser.getServerMessageOrDefault(errBody, "Tải file thất bại.")
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
