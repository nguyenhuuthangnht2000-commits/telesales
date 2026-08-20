package com.nhakhoaquangninh.telesales.call

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nhakhoaquangninh.telesales.OwnPhoneNumberResolver
import com.nhakhoaquangninh.telesales.ProcessCallWorker
import com.nhakhoaquangninh.telesales.data.local.FailedCallEvent
import com.nhakhoaquangninh.telesales.data.local.FailedCallEventManager
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.domain.model.CallMetadataMapper
import com.nhakhoaquangninh.telesales.domain.model.CallType
import com.nhakhoaquangninh.telesales.domain.model.FailureReason
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchResult
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

class CallEventCoordinator(
    context: Context,
    private val callLogDataSource: CallLogDataSource,
    private val recordingLocator: RecordingLocator,
    private val uploadScheduler: UploadScheduler,
    private val notifier: ComplianceNotifier
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val failedCallEvents = FailedCallEventManager.getInstance(appContext)

    fun enqueue(transition: CallTransition) {
        val snapshot = when (transition) {
            is CallTransition.ConnectedEnded -> transition.snapshot
            is CallTransition.MissedIncomingEnded -> transition.snapshot
            CallTransition.None -> return
        }
        val missedIncoming = transition is CallTransition.MissedIncomingEnded
        val input = Data.Builder()
            .putLong(ProcessCallWorker.KEY_SESSION_ID, snapshot.sessionId)
            .putBoolean(ProcessCallWorker.KEY_INCOMING, snapshot.incoming)
            .putString(ProcessCallWorker.KEY_OTHER_PHONE, snapshot.otherPhoneNumber)
            .putLong(ProcessCallWorker.KEY_STARTED_AT, snapshot.startedAtMillis)
            .putLong(ProcessCallWorker.KEY_ENDED_AT, snapshot.endedAtMillis)
            .putBoolean(ProcessCallWorker.KEY_MISSED_INCOMING, missedIncoming)
            .build()
        val request = OneTimeWorkRequestBuilder<ProcessCallWorker>()
            .setInputData(input)
            .setInitialDelay(
                if (missedIncoming) CALL_LOG_RETRY_DELAY_MILLIS else RECORDING_SETTLE_DELAY_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        workManager.enqueueUniqueWork(
            "Telesales_ProcessCall_${snapshot.sessionId}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    suspend fun process(snapshot: CallSessionSnapshot, missedIncoming: Boolean) {
        val callLog = awaitCallLog(snapshot, missedIncoming)
        if (missedIncoming) {
            saveFailedCall(
                snapshot = snapshot,
                callLog = callLog,
                callType = CallType.INCOMING,
                failureReason = FailureReason.MISSED
            )
            return
        }

        val match = if (callLog != null && callLog.durationSeconds > 0) {
            awaitRecordingMatch(callLog)
        } else {
            null
        }
        val decision = CallEventDecisionPolicy.decideConnected(
            snapshot = snapshot,
            callLog = callLog,
            attempt = MAX_CALL_LOG_ATTEMPTS,
            maxAttempts = MAX_CALL_LOG_ATTEMPTS,
            match = match
        )

        com.nhakhoaquangninh.telesales.core.FileLogger.log(
            appContext,
            "CALL_COORDINATOR",
            "Kết thúc xử lý cuộc gọi (SĐT: ${snapshot.otherPhoneNumber ?: callLog?.phoneNumber}, Chiều: ${if (snapshot.incoming) "ĐẾN" else "ĐI"}, Thời lượng CallLog: ${callLog?.durationSeconds ?: 0}s, Match: $match) -> Quyết định: $decision"
        )

        when (decision) {
            is CallEventDecision.SaveNotConnected -> saveFailedCall(
                snapshot = snapshot,
                callLog = decision.call,
                callType = CallType.OUTGOING,
                failureReason = FailureReason.NOT_CONNECTED
            )

            is CallEventDecision.ScheduleUpload -> {
                val currentCareType = TokenManager.getInstance(appContext).getSelectedCareTypeValue()
                val metadata = CallMetadataMapper.create(
                    recordingUri = decision.recording.uri,
                    callType = decision.call.callType,
                    otherPhoneNumber = decision.call.phoneNumber ?: snapshot.otherPhoneNumber,
                    ownPhoneNumber = OwnPhoneNumberResolver.resolve(appContext),
                    durationSeconds = decision.call.durationSeconds,
                    callAtFormatted = formatCallTime(decision.call.startedAtMillis),
                    isAnswered = true,
                    careType = currentCareType
                )
                uploadScheduler.enqueue(metadata)
                notifier.notifyRecordingQueued()
            }

            is CallEventDecision.NeedsReview -> {
                saveFailedCall(
                    snapshot = snapshot,
                    callLog = callLog,
                    callType = callLog?.callType ?: if (snapshot.incoming) CallType.INCOMING else CallType.OUTGOING,
                    failureReason = FailureReason.NOT_CONNECTED
                )
                notifier.notifyNeedsReview()
            }
            CallEventDecision.RecordingNotFound,
            CallEventDecision.RetryCallLog -> {
                saveFailedCall(
                    snapshot = snapshot,
                    callLog = callLog,
                    callType = callLog?.callType ?: if (snapshot.incoming) CallType.INCOMING else CallType.OUTGOING,
                    failureReason = FailureReason.NOT_CONNECTED
                )
                notifier.notifyMissingRecording()
            }
        }
    }

    private suspend fun awaitCallLog(
        snapshot: CallSessionSnapshot,
        missedIncoming: Boolean
    ): CallLogEntry? {
        var latest: CallLogEntry? = null
        for (attempt in 1..MAX_CALL_LOG_ATTEMPTS) {
            latest = callLogDataSource.findClosest(snapshot, missedIncoming)
            val waitingForOutgoingDuration =
                !missedIncoming && latest?.callType == CallType.OUTGOING && latest.durationSeconds == 0
            if (latest != null && !waitingForOutgoingDuration) return latest
            if (attempt < MAX_CALL_LOG_ATTEMPTS) delay(CALL_LOG_RETRY_DELAY_MILLIS)
        }
        return latest
    }

    private suspend fun awaitRecordingMatch(callLog: CallLogEntry): RecordingMatchResult {
        var lastResult: RecordingMatchResult = RecordingMatchResult.NotFound
        val retryDelays = listOf(0L, 3_000L, 7_000L, 15_000L, 25_000L)
        for ((index, delayMillis) in retryDelays.withIndex()) {
            if (delayMillis > 0L) {
                delay(delayMillis.milliseconds)
            }
            val match = recordingLocator.findMatch(callLog)
            lastResult = match
            if (match is RecordingMatchResult.Matched) {
                com.nhakhoaquangninh.telesales.core.FileLogger.log(
                    appContext,
                    "RECORDING_LOCATOR",
                    "Tìm thấy file ghi âm thành công ở lần thử ${index + 1}: ${match.recording.displayName} (${match.recording.uri})"
                )
                return match
            }
        }
        return lastResult
    }

    private fun saveFailedCall(
        snapshot: CallSessionSnapshot,
        callLog: CallLogEntry?,
        callType: CallType,
        failureReason: FailureReason
    ) {
        val eventTime = callLog?.startedAtMillis?.takeIf { it > 0L }
            ?: snapshot.startedAtMillis.takeIf { it > 0L }
            ?: System.currentTimeMillis()
        val currentCareType = TokenManager.getInstance(appContext).getSelectedCareTypeValue()
        val metadata = CallMetadataMapper.create(
            recordingUri = null,
            callType = callType,
            otherPhoneNumber = callLog?.phoneNumber ?: snapshot.otherPhoneNumber,
            ownPhoneNumber = OwnPhoneNumberResolver.resolve(appContext),
            durationSeconds = 0,
            callAtFormatted = formatCallTime(eventTime),
            isAnswered = false,
            careType = currentCareType
        )
        val otherPhone = if (callType == CallType.INCOMING) {
            metadata.phoneNumberFrom
        } else {
            metadata.phoneNumberTo
        }
        failedCallEvents.save(
            FailedCallEvent(
                id = "${eventTime}_${otherPhone ?: "unknown"}_${callType.wireValue}",
                phoneNumberFrom = metadata.phoneNumberFrom,
                phoneNumberTo = metadata.phoneNumberTo,
                callAtMillis = eventTime,
                callAtFormatted = requireNotNull(metadata.callAtFormatted),
                callType = callType,
                failureReason = failureReason
            )
        )
        notifier.notifyHistoryChanged()
        
        // Mới: Gửi tự động lên server
        uploadScheduler.enqueue(metadata)
    }

    private fun formatCallTime(timestamp: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }.format(Date(timestamp))

    private companion object {
        const val MAX_CALL_LOG_ATTEMPTS = 3
        const val CALL_LOG_RETRY_DELAY_MILLIS = 1_000L
        const val RECORDING_SETTLE_DELAY_MILLIS = 3_000L
    }
}
