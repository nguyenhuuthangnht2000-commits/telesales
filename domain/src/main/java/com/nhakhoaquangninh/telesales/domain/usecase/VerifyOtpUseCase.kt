package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository

class VerifyOtpUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(userId: Int, otp: String): Resource<UserSession> {
        val cleanOtp = otp.trim()

        if (cleanOtp.length != 6 || !cleanOtp.all { it.isDigit() }) {
            return Resource.Error(
                message = "Mã OTP phải gồm đúng 6 chữ số!",
                source = ErrorSource.APP_CLIENT
            )
        }

        return repository.verifyOtp(userId, cleanOtp)
    }
}
