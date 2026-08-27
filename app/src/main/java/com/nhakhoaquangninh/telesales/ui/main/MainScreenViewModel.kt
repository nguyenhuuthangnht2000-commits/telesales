package com.nhakhoaquangninh.telesales.ui.main

import android.content.Context
import android.util.Log
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.data.local.FailedCallEvent
import com.nhakhoaquangninh.telesales.data.local.FailedCallEventManager
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.SyncStatusManager
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallRecordingWindow
import com.nhakhoaquangninh.telesales.domain.model.CareTypeOption
import com.nhakhoaquangninh.telesales.domain.model.RecordingCandidate
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchPolicy
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchResult
import com.nhakhoaquangninh.telesales.domain.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

data class AudioItemState(
    val recording: RecordingCandidate,
    val status: SyncStatus,
    val metadata: CallRecordMetadata? = null
)

class MainScreenViewModel : BaseViewModel() {
    private val requestOtpUseCase = ServiceLocator.requestOtpUseCase
    private val verifyOtpUseCase = ServiceLocator.verifyOtpUseCase

    private val _selectedCareType = MutableStateFlow<CareTypeOption?>(null)
    val selectedCareType: StateFlow<CareTypeOption?> = _selectedCareType

    private val _audioFiles = MutableStateFlow<List<AudioItemState>>(emptyList())
    val audioFiles: StateFlow<List<AudioItemState>> = _audioFiles

    private val _failedCallEvents = MutableStateFlow<List<FailedCallEvent>>(emptyList())
    val failedCallEvents: StateFlow<List<FailedCallEvent>> = _failedCallEvents

    private val _callRecords = MutableStateFlow<List<com.nhakhoaquangninh.telesales.data.local.room.CallRecordEntity>>(emptyList())
    val callRecords: StateFlow<List<com.nhakhoaquangninh.telesales.data.local.room.CallRecordEntity>> = _callRecords

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _requestStopServiceOtpState = MutableStateFlow<Resource<String>>(Resource.Idle)
    val requestStopServiceOtpState: StateFlow<Resource<String>> = _requestStopServiceOtpState

    private val _verifyStopServiceOtpState = MutableStateFlow<Resource<UserSession>>(Resource.Idle)
    val verifyStopServiceOtpState: StateFlow<Resource<UserSession>> = _verifyStopServiceOtpState

    private val _stopServiceOtpInput = MutableStateFlow("")
    val stopServiceOtpInput: StateFlow<String> = _stopServiceOtpInput

    private val _stopServiceOtpError = MutableStateFlow<String?>(null)
    val stopServiceOtpError: StateFlow<String?> = _stopServiceOtpError

    fun initCareType(options: List<CareTypeOption>, savedValue: Int? = null, context: Context? = null) {
        if (options.isEmpty()) return
        if (_selectedCareType.value == null) {
            val found = options.find { it.value == savedValue } ?: options.first()
            _selectedCareType.value = found
            context?.let {
                TokenManager.getInstance(it).saveSelectedCareTypeValue(found.value)
            }
        }
    }

    fun onCareTypeSelected(option: CareTypeOption, context: Context? = null) {
        _selectedCareType.value = option
        context?.let {
            TokenManager.getInstance(it).saveSelectedCareTypeValue(option.value)
        }
    }

    fun onStopServiceOtpChanged(input: String) {
        if (input.length <= 6 && input.all { it.isDigit() }) {
            _stopServiceOtpInput.value = input
            if (_stopServiceOtpError.value != null) {
                _stopServiceOtpError.value = null
            }
        }
    }

    fun requestStopServiceOtp(userId: Int) {
        _requestStopServiceOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _requestStopServiceOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { requestOtpUseCase(userId.toString()) }
            _requestStopServiceOtpState.value = result
        }
    }

    fun verifyStopServiceOtp(userId: Int, context: Context, onSuccess: () -> Unit) {
        val otp = _stopServiceOtpInput.value
        _verifyStopServiceOtpState.value = Resource.Loading
        launchSafe(onError = { error -> _verifyStopServiceOtpState.value = error }) {
            val result = withContext(Dispatchers.IO) { verifyOtpUseCase(userId, otp) }
            if (result is Resource.Error && result.source == ErrorSource.APP_CLIENT) {
                _stopServiceOtpError.value = result.message
            } else if (result is Resource.Success) {
                TokenManager.getInstance(context).setMonitoringEnabled(false)
                onSuccess()
            }
            _verifyStopServiceOtpState.value = result
        }
    }

    fun resetRequestStopServiceOtpState() {
        _requestStopServiceOtpState.value = Resource.Idle
    }

    fun resetStopServiceOtpState() {
        _requestStopServiceOtpState.value = Resource.Idle
        _verifyStopServiceOtpState.value = Resource.Idle
        _stopServiceOtpInput.value = ""
        _stopServiceOtpError.value = null
    }

    fun loadFiles(context: Context) {
        val appContext = context.applicationContext
        launchSafe(onError = { Log.e(TAG, "Không thể tải lịch sử cuộc gọi") }) {
            ServiceLocator.init(appContext)
            val result = withContext(Dispatchers.IO) {
                val syncManager = SyncStatusManager.getInstance(appContext)
                val recordings = ServiceLocator.recordingRepository.getApprovedRecordings()
                val states = recordings.mapNotNull { recording ->
                    var metadata = syncManager.getMetadata(recording.uri)
                    var status = syncManager.getStatus(recording.uri)
                    if (metadata == null) {
                        val legacyMetadata = syncManager.getMetadata(recording.displayName)
                        if (legacyMetadata?.recordingUri == recording.uri) {
                            metadata = legacyMetadata
                            status = syncManager.getStatus(recording.displayName)
                            syncManager.setMetadata(recording.uri, status, legacyMetadata)
                            syncManager.removeStatus(recording.displayName)
                        }
                    }
                    if (metadata != null) {
                        AudioItemState(recording, status, metadata)
                    } else {
                        null
                    }
                }
                
                val currentUserId = TokenManager.getInstance(appContext).getUserId()
                val db = com.nhakhoaquangninh.telesales.data.local.room.TelesalesDatabase.getDatabase(appContext)
                val userRecords = db.callRecordDao().getByOwner(currentUserId)

                Triple(states, FailedCallEventManager.getInstance(appContext).getAll(), userRecords)
            }
            _audioFiles.value = result.first
            _failedCallEvents.value = result.second
            _callRecords.value = result.third
        }
    }

    fun deleteFailedCallEvent(context: Context, eventId: String) {
        val appContext = context.applicationContext
        launchSafe(onError = { Log.e(TAG, "Không thể xóa lịch sử cuộc gọi") }) {
            _failedCallEvents.value = withContext(Dispatchers.IO) {
                FailedCallEventManager.getInstance(appContext).run {
                    remove(eventId)
                    getAll()
                }
            }
        }
    }

    fun syncFiles(
        context: Context,
        files: List<AudioItemState>,
        onResult: (String, Boolean) -> Unit
    ) {
        val appContext = context.applicationContext
        launchSafe(onError = { error ->
            _isSyncing.value = false
            onResult(error.message, false)
        }) {
            _isSyncing.value = true
            ServiceLocator.init(appContext)
            val currentCareType = _selectedCareType.value?.value ?: TokenManager.getInstance(appContext).getSelectedCareTypeValue()
            val scheduledCount = withContext(Dispatchers.IO) {
                val syncManager = SyncStatusManager.getInstance(appContext)
                files.count { item ->
                    val enrichedMetadata = item.metadata?.copy(
                        careType = item.metadata.careType ?: currentCareType
                    )
                    if (isStrongUploadCandidate(item) && enrichedMetadata != null) {
                        ServiceLocator.uploadScheduler.enqueue(enrichedMetadata)
                        true
                    } else {
                        syncManager.setMetadata(
                            item.recording.uri,
                            SyncStatus.NEEDS_REVIEW,
                            enrichedMetadata ?: item.metadata
                        )
                        false
                    }
                }
            }
            loadFiles(appContext)
            _isSyncing.value = false
            if (scheduledCount == files.size && scheduledCount > 0) {
                onResult(appContext.getString(R.string.msg_upload_queued), true)
            } else {
                onResult(appContext.getString(R.string.msg_recording_needs_review), false)
            }
        }
    }

    private fun isStrongUploadCandidate(item: AudioItemState): Boolean {
        val metadata = item.metadata ?: return false
        if (metadata.recordingUri != item.recording.uri || metadata.durationSeconds <= 0) {
            return false
        }
        val endedAt = item.recording.modifiedAtMillis
        val durationMillis = metadata.durationSeconds * 1_000L
        val match = RecordingMatchPolicy.match(
            CallRecordingWindow(
                startedAtMillis = (endedAt - durationMillis).coerceAtLeast(0L),
                endedAtMillis = endedAt,
                durationSeconds = metadata.durationSeconds
            ),
            listOf(item.recording)
        )
        return match is RecordingMatchResult.Matched
    }

    private companion object {
        const val TAG = "MainScreenViewModel"
    }
}