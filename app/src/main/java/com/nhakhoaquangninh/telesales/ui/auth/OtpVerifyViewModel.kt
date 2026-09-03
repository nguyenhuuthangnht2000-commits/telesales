package com.nhakhoaquangninh.telesales.ui.auth

import android.content.Context
import com.nhakhoaquangninh.telesales.OwnPhoneNumberResolver
import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.data.local.room.TelesalesDatabase
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallMetadataMapper
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallType
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

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

    fun verifyOtp(userId: Int, context: Context) {
        val otp = _otpInput.value
        _uiState.value = Resource.Loading

        launchSafe(onError = { error -> _uiState.value = error }) {
            val result = withContext(Dispatchers.IO) { verifyOtpUseCase(userId, otp) }
            if (result is Resource.Error && result.source == ErrorSource.APP_CLIENT) {
                _otpError.value = result.message
            }
            if (result is Resource.Success) {
                clearInput()
                withContext(Dispatchers.IO) {
                    requeuePendingUploads(context.applicationContext)
                }
            }
            _uiState.value = result
        }
    }

    private fun requeuePendingUploads(context: Context) {
        val ownerUserId = ServiceLocator.tokenManager?.getUserId() ?: return
        if (ownerUserId == -1) return

        val pendingRecords = TelesalesDatabase.getDatabase(context)
            .callRecordDao()
            .getPendingByOwner(ownerUserId)
        val scheduler = ServiceLocator.uploadScheduler
        val ownPhoneNumber = OwnPhoneNumberResolver.resolve(context)

        pendingRecords.forEach { record ->
            val metadata = CallRecordMetadata(
                    phoneNumberFrom = record.phoneNumberFrom,
                    phoneNumberTo = record.phoneNumberTo,
                    callType = CallType.fromWire(record.callType) ?: CallType.OUTGOING,
                    durationSeconds = record.durationSeconds,
                    callAtFormatted = record.callAtFormatted,
                    recordingUri = record.recordingUri,
                    isAnswered = record.isAnswered,
                    careType = record.careType,
                    callId = record.callId ?: java.util.UUID.randomUUID().toString(),
                    ownerUserId = record.ownerUserId,
                    startedAtMillis = record.startedAtMillis
                )
            scheduler.enqueue(CallMetadataMapper.applyOwnPhoneNumber(metadata, ownPhoneNumber))
        }
    }

    private val requestOtpUseCase = ServiceLocator.requestOtpUseCase

    private val _resendState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val resendState: StateFlow<Resource<String>> = _resendState

    fun resendOtp(userId: Int) {
        _resendState.value = Resource.Loading
        launchSafe(onError = { error -> _resendState.value = error }) {
            val result = withContext(Dispatchers.IO) { requestOtpUseCase(userId.toString()) }
            _resendState.value = result
        }
    }

    fun resetResendState() {
        _resendState.value = Resource.Idle
    }

    fun clearInput() {
        _otpInput.value = ""
        _otpError.value = null
    }

    fun resetState() {
        _uiState.value = Resource.Idle
        _resendState.value = Resource.Idle
        clearInput()
    }
}
