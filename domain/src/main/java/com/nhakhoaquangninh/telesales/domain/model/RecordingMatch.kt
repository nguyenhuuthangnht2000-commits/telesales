package com.nhakhoaquangninh.telesales.domain.model

import kotlin.math.abs

data class RecordingCandidate(
    val uri: String,
    val displayName: String,
    val mimeType: String?,
    val relativePath: String?,
    val modifiedAtMillis: Long,
    val durationMillis: Long?,
    val sizeBytes: Long,
    val addedAtMillis: Long? = null
)

data class CallRecordingWindow(
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationSeconds: Int
)

enum class RecordingMatchConfidence { STRONG }

enum class RecordingReviewReason {
    SOURCE_NOT_APPROVED,
    AMBIGUOUS_SOURCE,
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
    /** Uses the matcher itself so diagnostics cannot drift from matching rules. */
    fun diagnosticReason(call: CallRecordingWindow, phoneNumber: String?, candidate: RecordingCandidate, fallback: Boolean): String {
        if (!fallback && candidate.modifiedAtMillis !in
            (call.endedAtMillis - EARLY_TOLERANCE_MILLIS)..(call.endedAtMillis + LATE_TOLERANCE_MILLIS)) {
            return "end_time_outside_window"
        }
        reviewReason(call, candidate)?.let { return it.name.lowercase() }
        if (fallback && matchFallback(call, phoneNumber, listOf(candidate)) == RecordingMatchResult.NotFound) {
            val phone = normalizePhoneToken(phoneNumber.orEmpty()) ?: return "call_phone_unavailable"
            val matches = Regex("\\+?\\d{10,13}").findAll(candidate.displayName).any {
                candidate.displayName.getOrNull(it.range.first - 1)?.isDigit() != true &&
                    candidate.displayName.getOrNull(it.range.last + 1)?.isDigit() != true &&
                    normalizePhoneToken(it.value) == phone
            }
            return if (!matches) "filename_phone_mismatch" else "fallback_time_outside_window"
        }
        return "eligible_requires_unique_match"
    }
    private const val EARLY_TOLERANCE_MILLIS = 15_000L
    private const val LATE_TOLERANCE_MILLIS = 75_000L
    private const val MIN_DURATION_TOLERANCE_MILLIS = 10_000L
    private const val DURATION_TOLERANCE_RATIO = 0.25
    const val FALLBACK_LATE_MILLIS = 90_000L
    const val FALLBACK_EARLY_MILLIS = 15_000L

    private val approvedPathMarkers = setOf(
        "recordings/call/",
        "recordings/call records/",
        "record/call/",
        "record/callrec/",
        "callrecordings/",
        "call_rec/",
        "callrec/",
        "call recording/",
        "call records/",
        "miui/sound_recorder/call_rec/",
        "sound_recorder/call_rec/",
        "vivo/callrecord/",
        "coloros/recorder/call/",
        "recorder/call/",
        "phonerecord/",
        "sounds/call/",
        "sounds/callrecorder/",
        "sounds/callrec/",
        "audio/call/"
    )

    private val ambiguousPathMarkers = setOf(
        "recordings/",
        "miui/sound_recorder/",
        "sound_recorder/",
        "vivo/record/",
        "coloros/recorder/",
        "recorder/",
        "voice recorder/",
        "sounds/",
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

    // Invoked only after the original end-time matcher returns NotFound.
    // Require a full phone token as extra evidence before considering other timestamps.
    fun matchFallback(
        call: CallRecordingWindow,
        phoneNumber: String?,
        candidates: List<RecordingCandidate>
    ): RecordingMatchResult {
        val phone = normalizePhoneToken(phoneNumber.orEmpty()) ?: return RecordingMatchResult.NotFound
        val eligible = candidates.distinctBy { it.uri }.filter { candidate ->
            val nameMatches = Regex("\\+?\\d{10,13}").findAll(candidate.displayName).any {
                val before = candidate.displayName.getOrNull(it.range.first - 1)
                val after = candidate.displayName.getOrNull(it.range.last + 1)
                before?.isDigit() != true && after?.isDigit() != true &&
                    normalizePhoneToken(it.value) == phone
            }
            val modifiedNearStart = candidate.modifiedAtMillis in
                (call.startedAtMillis - FALLBACK_EARLY_MILLIS)..(call.startedAtMillis + FALLBACK_EARLY_MILLIS)
            // A delayed index timestamp alone is not enough: it could belong to
            // another call to the same customer. Require it near a call boundary.
            val addedInWindow = candidate.addedAtMillis?.let {
                it in (call.startedAtMillis - FALLBACK_EARLY_MILLIS)..(call.startedAtMillis + FALLBACK_EARLY_MILLIS) ||
                    it in (call.endedAtMillis - EARLY_TOLERANCE_MILLIS)..(call.endedAtMillis + LATE_TOLERANCE_MILLIS)
            } == true
            nameMatches && (modifiedNearStart || addedInWindow) && reviewReason(call, candidate) == null
        }
        return when (eligible.size) {
            0 -> RecordingMatchResult.NotFound
            1 -> RecordingMatchResult.Matched(eligible.single(), RecordingMatchConfidence.STRONG)
            else -> RecordingMatchResult.NeedsReview(RecordingReviewReason.AMBIGUOUS_MATCH)
        }
    }

    private fun normalizePhoneToken(value: String): String? {
        val digits = value.filter(Char::isDigit)
        val local = if (digits.startsWith("84") && digits.length == 11) "0" + digits.drop(2) else digits
        return local.takeIf { it.length in 10..11 && it.startsWith("0") }
    }

    fun isApprovedSource(relativePath: String?): Boolean {
        val normalized = relativePath?.trim()?.replace('\\', '/')?.lowercase() ?: return false
        return approvedPathMarkers.any(normalized::contains)
    }

    fun isAmbiguousSource(relativePath: String?): Boolean {
        val normalized = relativePath?.trim()?.replace('\\', '/')?.lowercase() ?: return false
        return ambiguousPathMarkers.any(normalized::contains)
    }

    private fun reviewReason(
        call: CallRecordingWindow,
        candidate: RecordingCandidate
    ): RecordingReviewReason? {
        if (!isApprovedSource(candidate.relativePath)) {
            if (isAmbiguousSource(candidate.relativePath)) {
                return RecordingReviewReason.AMBIGUOUS_SOURCE
            }
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
