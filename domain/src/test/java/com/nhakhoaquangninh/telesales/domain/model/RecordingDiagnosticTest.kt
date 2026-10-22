package com.nhakhoaquangninh.telesales.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RecordingDiagnosticTest {
    private val call = CallRecordingWindow(100_000L, 130_000L, 30)
    private val candidate = RecordingCandidate(
        uri = "content://media/external/audio/media/1",
        displayName = "Call_0901234567.m4a",
        mimeType = "audio/mp4",
        relativePath = "Recordings/Call/",
        modifiedAtMillis = 130_000L,
        durationMillis = 30_000L,
        sizeBytes = 100_000L
    )

    @Test fun outsideOriginalWindowIsExplained() {
        assertEquals("end_time_outside_window", RecordingMatchPolicy.diagnosticReason(
            call, "0901234567", candidate.copy(modifiedAtMillis = 10_000L), false))
    }

    @Test fun invalidAudioIsExplainedWithoutChangingMatch() {
        val invalid = candidate.copy(sizeBytes = 0)
        assertEquals("invalid_size", RecordingMatchPolicy.diagnosticReason(call, "0901234567", invalid, false))
        assertEquals(RecordingMatchResult.NeedsReview(RecordingReviewReason.INVALID_SIZE),
            RecordingMatchPolicy.match(call, listOf(invalid)))
    }

    @Test fun fallbackPhoneMismatchIsDistinguishedFromTimeMismatch() {
        assertEquals("filename_phone_mismatch", RecordingMatchPolicy.diagnosticReason(
            call, "0987654321", candidate, true))
        assertEquals("fallback_time_outside_window", RecordingMatchPolicy.diagnosticReason(
            call, "0901234567", candidate, true))
    }

    @Test fun fallbackEligibleCandidateStillRequiresUniqueMatch() {
        assertEquals("eligible_requires_unique_match", RecordingMatchPolicy.diagnosticReason(
            call, "0901234567", candidate.copy(addedAtMillis = 130_000L), true))
    }
}
