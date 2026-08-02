package com.nhakhoaquangninh.telesales.domain.common

enum class ErrorSource {
    APP_CLIENT,   // Lỗi từ phía Ứng dụng (Validation, dữ liệu không hợp lệ)
    NETWORK,      // Lỗi Kết nối Mạng (Mất internet, Timeout, Host)
    SERVER        // Lỗi từ phía Máy chủ (401, 404, 422, 500...)
}

sealed class Resource<out T> {
    data object Idle : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    data class Success<out T>(val data: T, val message: String? = null) : Resource<T>()
    data class Error(
        val message: String,
        val source: ErrorSource = ErrorSource.APP_CLIENT,
        val code: Int? = null,
        val rawDetails: String? = null
    ) : Resource<Nothing>()
}
