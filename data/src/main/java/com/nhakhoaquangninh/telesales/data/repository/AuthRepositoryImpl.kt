package com.nhakhoaquangninh.telesales.data.repository

import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.remote.ApiService
import com.nhakhoaquangninh.telesales.data.remote.RetrofitClient
import com.nhakhoaquangninh.telesales.data.remote.dto.RequestOtpRequest
import com.nhakhoaquangninh.telesales.data.remote.dto.VerifyOtpRequest
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val tokenManager: TokenManager
) : AuthRepository {

    override suspend fun requestOtp(userId: Int): Resource<String> {
        val response = apiService.requestOtp(
            apiKey = RetrofitClient.DEFAULT_API_KEY,
            request = RequestOtpRequest(userId = userId)
        )

        val code = response.code()
        return if (response.isSuccessful && code == 200) {
            val msg = response.body()?.message ?: "Mã OTP đã được gửi về email của quản lý."
            Resource.Success(data = msg)
        } else {
            val errBody = response.errorBody()?.string()
            val fallback = when (code) {
                401 -> "API Key không hợp lệ!"
                404 -> "Không tìm thấy nhân viên ID $userId trên hệ thống!"
                500 -> "Máy chủ chưa cấu hình gửi Email hoặc bị lỗi nội bộ."
                else -> "Yêu cầu OTP thất bại."
            }
            val message = ApiErrorParser.getServerMessageOrDefault(errBody, fallback)
            Resource.Error(
                message = message,
                source = ErrorSource.SERVER,
                code = code,
                rawDetails = errBody
            )
        }
    }

    override suspend fun verifyOtp(userId: Int, otp: String): Resource<UserSession> {
        val response = apiService.verifyOtp(
            apiKey = RetrofitClient.DEFAULT_API_KEY,
            request = VerifyOtpRequest(userId = userId, otp = otp)
        )

        val code = response.code()
        return if (response.isSuccessful && code == 200) {
            val baseResp = response.body()
            val data = baseResp?.data
            val token = data?.token
            val user = data?.user
            if (!token.isNullOrEmpty()) {
                val session = UserSession(
                    userId = userId,
                    token = token,
                    userName = user?.name,
                    email = user?.email
                )
                tokenManager.saveSession(session)
                Resource.Success(data = session, message = baseResp.message)
            } else {
                Resource.Error(
                    message = "Server không trả về Token xác thực!",
                    source = ErrorSource.SERVER
                )
            }
        } else {
            val errBody = response.errorBody()?.string()
            val fallback = when (code) {
                401 -> "Mã OTP không chính xác hoặc đã hết hạn (15 phút)!"
                404 -> "Không tìm thấy thông tin nhân viên!"
                else -> "Xác thực OTP thất bại."
            }
            val message = ApiErrorParser.getServerMessageOrDefault(errBody, fallback)
            Resource.Error(
                message = message,
                source = ErrorSource.SERVER,
                code = code,
                rawDetails = errBody
            )
        }
    }

    override fun getSavedSession(): UserSession? = tokenManager.getSession()

    override fun isLoggedIn(): Boolean = tokenManager.isLoggedIn()

    override fun clearSession() = tokenManager.clearSession()
}
