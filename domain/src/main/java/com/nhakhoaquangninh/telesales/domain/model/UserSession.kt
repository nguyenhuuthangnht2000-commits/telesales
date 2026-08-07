package com.nhakhoaquangninh.telesales.domain.model

data class UserSession(
    val userId: Int,
    val token: String,
    val userName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null
)
