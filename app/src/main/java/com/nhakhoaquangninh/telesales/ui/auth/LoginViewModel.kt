package com.nhakhoaquangninh.telesales.ui.auth

import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginViewModel : BaseViewModel() {

    private val requestOtpUseCase = ServiceLocator.requestOtpUseCase

    private val _userIdInput = MutableStateFlow("")
    val userIdInput: StateFlow<String> = _userIdInput

    private val _userIdError = MutableStateFlow<String?>(null)
    val userIdError: StateFlow<String?> = _userIdError

    private val _uiState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val uiState: StateFlow<Resource<String>> = _uiState

    fun onUserIdChanged(input: String) {
        _userIdInput.value = input
        if (_userIdError.value != null) {
            _userIdError.value = null
        }
    }

    fun requestOtp() {
        val input = _userIdInput.value
        _uiState.value = Resource.Loading

        launchSafe(onError = { error -> _uiState.value = error }) {
            val result = withContext(Dispatchers.IO) { requestOtpUseCase(input) }
            if (result is Resource.Error && result.source == ErrorSource.APP_CLIENT) {
                _userIdError.value = result.message
            }
            _uiState.value = result
        }
    }

    fun resetState() {
        _uiState.value = Resource.Idle
    }
}
