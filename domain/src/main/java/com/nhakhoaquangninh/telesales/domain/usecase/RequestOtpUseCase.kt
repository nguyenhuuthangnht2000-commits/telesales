package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository

class RequestOtpUseCase(private val repository: AuthRepository) {

    suspend operator fun invoke(userIdString: String): Resource<String> {
        val input = userIdString.trim()

        if (input.isEmpty()) {
            return Resource.Error(
                message = "Bạn chưa nhập ID nhân viên!",
                source = ErrorSource.APP_CLIENT
            )
        }

        val userIdInt = input.toIntOrNull()
        if (userIdInt == null || userIdInt <= 0) {
            return Resource.Error(
                message = "ID nhân viên '\$input' không hợp lệ. Phải là số nguyên > 0.",
                source = ErrorSource.APP_CLIENT
            )
        }

        return repository.requestOtp(userIdInt)
    }
}
