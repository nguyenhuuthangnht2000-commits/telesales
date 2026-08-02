package com.nhakhoaquangninh.telesales.domain.repository

import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.UserSession

interface AuthRepository {
    suspend fun requestOtp(userId: Int): Resource<String>
    suspend fun verifyOtp(userId: Int, otp: String): Resource<UserSession>
    fun getSavedSession(): UserSession?
    fun isLoggedIn(): Boolean
    fun clearSession()
}
