package com.nhakhoaquangninh.telesales.ui.main

import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsViewModel : BaseViewModel() {
    private val requestOtpUseCase = ServiceLocator.requestOtpUseCase
    private val verifyOtpUseCase = ServiceLocator.verifyOtpUseCase

    private val _requestOtpState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val requestOtpState: StateFlow<Resource<String>> = _requestOtpState

    private val _verifyOtpState = MutableStateFlow<Resource<UserSession>>(Resource.Idle)
    val verifyOtpState: StateFlow<Resource<UserSession>> = _verifyOtpState

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError

    fun onOtpChanged(input: String) {
        if (input.length <= 6 && input.all { it.isDigit() }) {
            _otpInput.value = input
            if (_otpError.value != null) {
                _otpError.value = null
            }
        }
    }

    fun requestOtp(userId: Int) {
        _requestOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _requestOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { requestOtpUseCase(userId.toString()) }
            _requestOtpState.value = result
        }
    }

    fun verifyOtp(userId: Int) {
        val otp = _otpInput.value
        _verifyOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _verifyOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { verifyOtpUseCase(userId, otp) }
            if (result is Resource.Error && result.source == ErrorSource.APP_CLIENT) {
                _otpError.value = result.message
            }
            _verifyOtpState.value = result
        }
    }

    fun resetRequestOtpState() {
        _requestOtpState.value = Resource.Idle
    }

    fun resetVerifyOtpState() {
        _verifyOtpState.value = Resource.Idle
        _otpInput.value = ""
        _otpError.value = null
    }
}
