package com.nhakhoaquangninh.telesales.data.repository

import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.remote.ApiService
import com.nhakhoaquangninh.telesales.data.remote.RetrofitClient
import com.nhakhoaquangninh.telesales.data.remote.dto.RequestOtpRequest
import com.nhakhoaquangninh.telesales.data.remote.dto.VerifyOtpRequest
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.MessageProvider
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val tokenManager: TokenManager,
    private val messageProvider: MessageProvider
) : AuthRepository {

    override suspend fun requestOtp(userId: Int): Resource<String> {
        val response = apiService.requestOtp(
            apiKey = RetrofitClient.DEFAULT_API_KEY,
            request = RequestOtpRequest(userId = userId)
        )

        val code = response.code()
        return if (response.isSuccessful && code == 200) {
            val msg = response.body()?.message ?: messageProvider.getOtpSentMessage()
            Resource.Success(data = msg)
        } else {
            val errBody = response.errorBody()?.string()
            val fallback = when (code) {
                401 -> messageProvider.getApiKeyInvalidMessage()
                404 -> messageProvider.getUserNotFoundMessage(userId)
                500 -> messageProvider.getServerErrorMessage()
                else -> messageProvider.getOtpRequestFailedMessage()
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
                    message = messageProvider.getServerNoTokenMessage(),
                    source = ErrorSource.SERVER
                )
            }
        } else {
            val errBody = response.errorBody()?.string()
            val fallback = when (code) {
                401 -> messageProvider.getOtpInvalidMessage()
                404 -> messageProvider.getUserInfoMissingMessage()
                else -> messageProvider.getOtpVerifyFailedMessage()
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
