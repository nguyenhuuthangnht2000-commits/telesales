package com.nhakhoaquangninh.telesales.ui.main

import android.content.Context
import android.util.Log
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.ServiceLocator
import com.nhakhoaquangninh.telesales.call.CallLogDataSource
import com.nhakhoaquangninh.telesales.core.BaseViewModel
import com.nhakhoaquangninh.telesales.data.local.FailedCallEvent
import com.nhakhoaquangninh.telesales.data.local.FailedCallEventManager
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.SyncStatusManager
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallRecordingWindow
import com.nhakhoaquangninh.telesales.domain.model.RecordingCandidate
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchPolicy
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchResult
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
    private val _audioFiles = MutableStateFlow<List<AudioItemState>>(emptyList())
    val audioFiles: StateFlow<List<AudioItemState>> = _audioFiles

    private val _failedCallEvents = MutableStateFlow<List<FailedCallEvent>>(emptyList())
    val failedCallEvents: StateFlow<List<FailedCallEvent>> = _failedCallEvents

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    fun loadFiles(context: Context) {
        val appContext = context.applicationContext
        launchSafe(onError = { Log.e(TAG, "Không thể tải lịch sử cuộc gọi") }) {
            ServiceLocator.init(appContext)
            val result = withContext(Dispatchers.IO) {
                val syncManager = SyncStatusManager.getInstance(appContext)
                val recordings = ServiceLocator.recordingRepository.getApprovedRecordings()
                val states = recordings.map { recording ->
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
                    if (metadata == null && status == SyncStatus.PENDING) {
                        val duration = recording.durationMillis
                        if (duration != null) {
                            val callLogDataSource = CallLogDataSource(appContext)
                            val recovered = callLogDataSource.recoverFromTime(recording.modifiedAtMillis, (duration / 1000).toInt())
                            if (recovered != null) {
                                val ownPhone = com.nhakhoaquangninh.telesales.OwnPhoneNumberResolver.resolve(appContext)
                                val callAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                                    timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
                                }.format(java.util.Date(recovered.startedAtMillis))
                                metadata = com.nhakhoaquangninh.telesales.domain.model.CallMetadataMapper.create(
                                    recordingUri = recording.uri,
                                    callType = recovered.callType,
                                    otherPhoneNumber = recovered.phoneNumber,
                                    ownPhoneNumber = ownPhone,
                                    durationSeconds = recovered.durationSeconds,
                                    callAtFormatted = callAt
                                )
                                syncManager.setMetadata(recording.uri, SyncStatus.PENDING, metadata)
                            }
                        }
                        if (metadata == null) {
                            status = SyncStatus.NEEDS_REVIEW
                        }
                    }
                    AudioItemState(recording, status, metadata)
                }
                states to FailedCallEventManager.getInstance(appContext).getAll()
            }
            _audioFiles.value = result.first
            _failedCallEvents.value = result.second
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
            val scheduledCount = withContext(Dispatchers.IO) {
                val syncManager = SyncStatusManager.getInstance(appContext)
                files.count { item ->
                    if (isStrongUploadCandidate(item)) {
                        ServiceLocator.uploadScheduler.enqueue(requireNotNull(item.metadata))
                        true
                    } else {
                        syncManager.setMetadata(
                            item.recording.uri,
                            SyncStatus.NEEDS_REVIEW,
                            item.metadata
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