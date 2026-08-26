package com.nhakhoaquangninh.telesales.domain.model

import kotlin.math.abs

data class RecordingCandidate(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val relativePath: String?,
    val modifiedAtMillis: Long,
    val durationMillis: Long?,
    val sizeBytes: Long
)

data class CallRecordingWindow(
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationSeconds: Int
)

enum class RecordingMatchConfidence { STRONG }

enum class RecordingReviewReason {
    SOURCE_NOT_APPROVED,
    MIME_NOT_AUDIO,
    DURATION_UNAVAILABLE,
    DURATION_MISMATCH,
    INVALID_SIZE,
    AMBIGUOUS_MATCH
}

sealed interface RecordingMatchResult {
    data class Matched(
        val recording: RecordingCandidate,
        val confidence: RecordingMatchConfidence
    ) : RecordingMatchResult

    data class NeedsReview(val reason: RecordingReviewReason) : RecordingMatchResult

    data object NotFound : RecordingMatchResult
}

object RecordingMatchPolicy {
    private const val EARLY_TOLERANCE_MILLIS = 15_000L
    private const val LATE_TOLERANCE_MILLIS = 75_000L
    private const val MIN_DURATION_TOLERANCE_MILLIS = 10_000L
    private const val DURATION_TOLERANCE_RATIO = 0.25

    private val approvedPathMarkers = setOf(
        "recordings/call/",
        "recordings/call records/",
        "recordings/",
        "record/call/",
        "record/callrec/",
        "callrecordings/",
        "call_rec/",
        "callrec/",
        "call recording/",
        "call records/",
        "miui/sound_recorder/call_rec/",
        "miui/sound_recorder/",
        "sound_recorder/call_rec/",
        "sound_recorder/",
        "vivo/callrecord/",
        "vivo/record/",
        "coloros/recorder/call/",
        "coloros/recorder/",
        "recorder/call/",
        "recorder/",
        "phonerecord/",
        "voice recorder/",
        "sounds/call/",
        "sounds/callrecorder/",
        "sounds/callrec/",
        "sounds/",
        "audio/call/",
        "audio/recordings/",
        "music/recordings/",
        "voice/"
    )

    fun match(
        call: CallRecordingWindow,
        candidates: List<RecordingCandidate>
    ): RecordingMatchResult {
        val temporalCandidates = candidates.filter {
            it.modifiedAtMillis in
                (call.endedAtMillis - EARLY_TOLERANCE_MILLIS)..
                    (call.endedAtMillis + LATE_TOLERANCE_MILLIS)
        }
        if (temporalCandidates.isEmpty()) return RecordingMatchResult.NotFound

        val assessed = temporalCandidates.map { it to reviewReason(call, it) }
        val strong = assessed.filter { it.second == null }.map { it.first }
        if (strong.size > 1) {
            return RecordingMatchResult.NeedsReview(RecordingReviewReason.AMBIGUOUS_MATCH)
        }
        if (strong.size == 1) {
            return RecordingMatchResult.Matched(strong.single(), RecordingMatchConfidence.STRONG)
        }
        return RecordingMatchResult.NeedsReview(
            assessed.firstNotNullOf { it.second }
        )
    }

    fun isApprovedSource(relativePath: String?): Boolean {
        val normalized = relativePath?.trim()?.replace('\\', '/')?.lowercase() ?: return false
        return approvedPathMarkers.any(normalized::contains)
    }

    private fun reviewReason(
        call: CallRecordingWindow,
        candidate: RecordingCandidate
    ): RecordingReviewReason? {
        if (!isApprovedSource(candidate.relativePath)) {
            return RecordingReviewReason.SOURCE_NOT_APPROVED
        }
        if (candidate.mimeType?.lowercase()?.startsWith("audio/") != true) {
            return RecordingReviewReason.MIME_NOT_AUDIO
        }
        if (candidate.sizeBytes <= 0L) return RecordingReviewReason.INVALID_SIZE
        val recordingDuration = candidate.durationMillis
            ?.takeIf { it > 0L }
            ?: return RecordingReviewReason.DURATION_UNAVAILABLE
        val callDuration = call.durationSeconds.coerceAtLeast(0) * 1_000L
        if (callDuration <= 0L) return RecordingReviewReason.DURATION_MISMATCH
        val tolerance = maxOf(
            MIN_DURATION_TOLERANCE_MILLIS,
            (callDuration * DURATION_TOLERANCE_RATIO).toLong()
        )
        if (abs(recordingDuration - callDuration) > tolerance) {
            return RecordingReviewReason.DURATION_MISMATCH
        }
        return null
    }
}
