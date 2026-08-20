package com.nhakhoaquangninh.telesales.data.repository

import com.nhakhoaquangninh.telesales.core.FileLogger
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.data.remote.ApiService
import com.nhakhoaquangninh.telesales.data.remote.RetrofitClient
import com.nhakhoaquangninh.telesales.data.remote.dto.LogoutRequest
import com.nhakhoaquangninh.telesales.data.remote.dto.RequestOtpRequest
import com.nhakhoaquangninh.telesales.data.remote.dto.VerifyOtpRequest
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.MessageProvider
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CareTypeOption
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
                val rawCareOptions = data.careTypeOptions ?: user?.careTypeOptions
                val session = UserSession(
                    userId = userId,
                    token = token,
                    userName = user?.name,
                    email = user?.email,
                    phoneNumber = user?.phone?.trim()?.takeIf { it.isNotEmpty() },
                    careTypeOptions = rawCareOptions?.map {
                        CareTypeOption(
                            value = it.value,
                            label = it.label
                        )
                    } ?: emptyList()
                )
                tokenManager.saveSession(session)
                if (session.careTypeOptions.isNotEmpty() && tokenManager.getSelectedCareTypeValue() == null) {
                    tokenManager.saveSelectedCareTypeValue(session.careTypeOptions.first().value)
                }
                FileLogger.setUserId(userId.toString())
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

    override suspend fun requestLogoutOtp(): Resource<String> {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            return Resource.Error(
                message = messageProvider.getTokenMissingMessage(),
                source = ErrorSource.SERVER,
                code = 401
            )
        }

        val response = apiService.requestLogoutOtp(
            apiKey = RetrofitClient.DEFAULT_API_KEY,
            authorization = "Bearer $token"
        )

        val code = response.code()
        return if (response.isSuccessful && code == 200) {
            val msg = response.body()?.message ?: messageProvider.getLogoutOtpSentMessage()
            Resource.Success(data = msg)
        } else {
            val errBody = response.errorBody()?.string()
            val fallback = when (code) {
                401 -> messageProvider.getTokenExpiredMessage()
                500 -> messageProvider.getServerErrorMessage()
                else -> messageProvider.getLogoutOtpRequestFailedMessage()
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

    override suspend fun logout(otp: String): Resource<Boolean> {
        val token = tokenManager.getToken()
        if (token.isNullOrEmpty()) {
            return Resource.Error(
                message = messageProvider.getTokenMissingMessage(),
                source = ErrorSource.SERVER,
                code = 401
            )
        }

        if (otp.length != 6 || !otp.all { it.isDigit() }) {
            return Resource.Error(
                message = messageProvider.getOtpInvalidMessage(),
                source = ErrorSource.APP_CLIENT
            )
        }

        val response = apiService.logout(
            apiKey = RetrofitClient.DEFAULT_API_KEY,
            authorization = "Bearer $token",
            request = LogoutRequest(otp = otp)
        )

        val code = response.code()
        return if (response.isSuccessful && code == 200) {
            clearSession()
            val msg = response.body()?.message ?: messageProvider.getLogoutSuccessMessage()
            Resource.Success(data = true, message = msg)
        } else {
            val errBody = response.errorBody()?.string()
            val fallback = when (code) {
                401 -> messageProvider.getOtpInvalidMessage()
                422 -> messageProvider.getOtpInvalidMessage()
                else -> messageProvider.getLogoutFailedMessage()
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

    override fun clearSession() {
        FileLogger.setUserId("")
        tokenManager.clearSession()
    }
}
