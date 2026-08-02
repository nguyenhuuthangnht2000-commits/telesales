package com.nhakhoaquangninh.telesales.ui.auth

import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OtpVerifyViewModel : BaseViewModel() {

    private val verifyOtpUseCase = ServiceLocator.verifyOtpUseCase

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError

    private val _uiState = MutableStateFlow<Resource<UserSession>>(Resource.Idle)
    val uiState: StateFlow<Resource<UserSession>> = _uiState

    fun onOtpChanged(input: String) {
        if (input.length <= 6 && input.all { it.isDigit() }) {
            _otpInput.value = input
            if (_otpError.value != null) {
                _otpError.value = null
            }
        }
    }

    fun verifyOtp(userId: Int) {
        val otp = _otpInput.value
        _uiState.value = Resource.Loading

        launchSafe(onError = { error -> _uiState.value = error }) {
            val result = verifyOtpUseCase(userId, otp)
            if (result is Resource.Error && result.source == ErrorSource.APP_CLIENT) {
                _otpError.value = result.message
            }
            _uiState.value = result
        }
    }

    fun resetState() {
        _uiState.value = Resource.Idle
    }
}
