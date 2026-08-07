package com.nhakhoaquangninh.telesales.call

import com.nhakhoaquangninh.telesales.domain.model.CallType
import com.nhakhoaquangninh.telesales.domain.model.RecordingCandidate
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchConfidence
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchResult
import com.nhakhoaquangninh.telesales.domain.model.RecordingReviewReason

data class CallLogEntry(
    val phoneNumber: String?,
    val callType: CallType,
    val startedAtMillis: Long,
    val durationSeconds: Int
) {
    val endedAtMillis: Long
        get() = startedAtMillis + durationSeconds.coerceAtLeast(0) * 1_000L
}

sealed interface CallEventDecision {
    data object RetryCallLog : CallEventDecision
    data class SaveNotConnected(val call: CallLogEntry) : CallEventDecision
    data class ScheduleUpload(
        val call: CallLogEntry,
        val recording: RecordingCandidate
    ) : CallEventDecision

    data class NeedsReview(val reason: RecordingReviewReason) : CallEventDecision
    data object RecordingNotFound : CallEventDecision
}

object CallEventDecisionPolicy {
    fun decideConnected(
        snapshot: CallSessionSnapshot,
        callLog: CallLogEntry?,
        attempt: Int,
        maxAttempts: Int,
        match: RecordingMatchResult?
    ): CallEventDecision {
        if (callLog == null) {
            return if (attempt < maxAttempts) {
                CallEventDecision.RetryCallLog
            } else {
                CallEventDecision.RecordingNotFound
            }
        }
        if (callLog.callType == CallType.OUTGOING && callLog.durationSeconds == 0) {
            return if (attempt < maxAttempts) {
                CallEventDecision.RetryCallLog
            } else {
                CallEventDecision.SaveNotConnected(callLog)
            }
        }
        return when (match) {
            is RecordingMatchResult.Matched -> {
                if (match.confidence == RecordingMatchConfidence.STRONG) {
                    CallEventDecision.ScheduleUpload(callLog, match.recording)
                } else {
                    CallEventDecision.NeedsReview(RecordingReviewReason.AMBIGUOUS_MATCH)
                }
            }

            is RecordingMatchResult.NeedsReview -> CallEventDecision.NeedsReview(match.reason)
            RecordingMatchResult.NotFound, null -> CallEventDecision.RecordingNotFound
        }
    }
}
