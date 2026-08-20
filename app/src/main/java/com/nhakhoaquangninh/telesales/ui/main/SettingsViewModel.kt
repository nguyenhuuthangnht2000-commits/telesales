package com.nhakhoaquangninh.telesales.ui.main

import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

class SettingsViewModel : BaseViewModel() {
    private val requestLogoutOtpUseCase = ServiceLocator.requestLogoutOtpUseCase
    private val logoutUseCase = ServiceLocator.logoutUseCase
    private val requestOtpUseCase = ServiceLocator.requestOtpUseCase
    private val verifyOtpUseCase = ServiceLocator.verifyOtpUseCase

    // ── Logout OTP Flow ──
    private val _requestOtpState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val requestOtpState: StateFlow<Resource<String>> = _requestOtpState

    private val _verifyOtpState = MutableStateFlow<Resource<Boolean>>(Resource.Idle)
    val verifyOtpState: StateFlow<Resource<Boolean>> = _verifyOtpState

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput

    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError

    // ── Clear Log OTP Flow ──
    private val _requestClearLogOtpState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val requestClearLogOtpState: StateFlow<Resource<String>> = _requestClearLogOtpState

    private val _verifyClearLogOtpState = MutableStateFlow<Resource<UserSession>>(Resource.Idle)
    val verifyClearLogOtpState: StateFlow<Resource<UserSession>> = _verifyClearLogOtpState

    private val _clearLogOtpInput = MutableStateFlow("")
    val clearLogOtpInput: StateFlow<String> = _clearLogOtpInput

    private val _clearLogOtpError = MutableStateFlow<String?>(null)
    val clearLogOtpError: StateFlow<String?> = _clearLogOtpError

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

    fun resetRequestOtpState() {
        _requestOtpState.value = Resource.Idle
    }

    fun resetVerifyOtpState() {
        _verifyOtpState.value = Resource.Idle
        _otpInput.value = ""
        _otpError.value = null
    }

    // ── Clear Log Methods ──
    fun onClearLogOtpChanged(input: String) {
        if (input.length <= 6 && input.all { it.isDigit() }) {
            _clearLogOtpInput.value = input
            if (_clearLogOtpError.value != null) {
                _clearLogOtpError.value = null
            }
        }
    }

    fun requestClearLogOtp(userId: Int) {
        _requestClearLogOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _requestClearLogOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { requestOtpUseCase(userId.toString()) }
            _requestClearLogOtpState.value = result
        }
    }

    fun verifyClearLogOtp(userId: Int, context: android.content.Context, onSuccess: () -> Unit) {
        val otp = _clearLogOtpInput.value
        _verifyClearLogOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _verifyClearLogOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { verifyOtpUseCase(userId, otp) }
            if (result is Resource.Error && result.source == ErrorSource.APP_CLIENT) {
                _clearLogOtpError.value = result.message
            } else if (result is Resource.Success) {
                withContext(Dispatchers.IO) {
                    com.nhakhoaquangninh.telesales.core.FileLogger.clearLog(context)
                }
                onSuccess()
            }
            _verifyClearLogOtpState.value = result
        }
    }

    fun resetRequestClearLogOtpState() {
        _requestClearLogOtpState.value = Resource.Idle
    }

    fun resetClearLogOtpState() {
        _requestClearLogOtpState.value = Resource.Idle
        _verifyClearLogOtpState.value = Resource.Idle
        _clearLogOtpInput.value = ""
        _clearLogOtpError.value = null
    }
}
