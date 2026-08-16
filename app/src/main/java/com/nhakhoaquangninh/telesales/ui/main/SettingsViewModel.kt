package com.nhakhoaquangninh.telesales.ui.main

import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class SettingsViewModel : BaseViewModel() {
    private val requestLogoutOtpUseCase = ServiceLocator.requestLogoutOtpUseCase
    private val logoutUseCase = ServiceLocator.logoutUseCase

    private val _requestOtpState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val requestOtpState: StateFlow<Resource<String>> = _requestOtpState

    private val _verifyOtpState = MutableStateFlow<Resource<Boolean>>(Resource.Idle)
    val verifyOtpState: StateFlow<Resource<Boolean>> = _verifyOtpState

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

    fun requestLogoutOtp() {
        _requestOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _requestOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { requestLogoutOtpUseCase() }
            _requestOtpState.value = result
        }
    }

    fun requestOtp(userId: Int = 0) {
        requestLogoutOtp()
    }

    fun verifyLogoutOtp() {
        val otp = _otpInput.value
        _verifyOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _verifyOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { logoutUseCase(otp) }
            if (result is Resource.Error && result.source == ErrorSource.APP_CLIENT) {
                _otpError.value = result.message
            }
            _verifyOtpState.value = result
        }
    }

    fun verifyOtp(userId: Int = 0) {
        verifyLogoutOtp()
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
