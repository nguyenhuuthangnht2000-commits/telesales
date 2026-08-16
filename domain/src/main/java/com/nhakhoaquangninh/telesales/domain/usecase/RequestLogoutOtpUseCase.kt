package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository

class RequestLogoutOtpUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Resource<String> = repository.requestLogoutOtp()
}
