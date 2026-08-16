package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository

class LogoutUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(otp: String): Resource<Boolean> = repository.logout(otp)
}
