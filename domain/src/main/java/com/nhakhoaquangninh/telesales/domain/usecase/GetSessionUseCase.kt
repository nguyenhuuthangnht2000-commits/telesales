package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.model.UserSession
import com.nhakhoaquangninh.telesales.domain.repository.AuthRepository

class GetSessionUseCase(private val repository: AuthRepository) {
    fun isLoggedIn(): Boolean = repository.isLoggedIn()
    fun getSession(): UserSession? = repository.getSavedSession()
    fun logout() = repository.clearSession()
}
