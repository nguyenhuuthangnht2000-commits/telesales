package com.nhakhoaquangninh.telesales

import com.google.common.truth.Truth.assertThat
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test

class DailyCallCleanupPolicyTest {

    @Test
    fun startOfCurrentDay_usesVietnamTimeZone() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.AUGUST, 27, 18, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val expected = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")).apply {
            set(2026, Calendar.AUGUST, 28, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertThat(DailyCallCleanupPolicy.startOfCurrentDayMillis(now)).isEqualTo(expected)
    }

    @Test
    fun nextCleanupDelay_waitsUntilTheFollowingVietnamMidnight() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh")).apply {
            set(2026, Calendar.AUGUST, 27, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertThat(DailyCallCleanupPolicy.nextCleanupDelayMillis(now)).isEqualTo(60_000L)
    }

    @Test
    fun cleanupDayKey_usesVietnamCalendarDay() {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.AUGUST, 27, 18, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertThat(DailyCallCleanupPolicy.cleanupDayKey(now)).isEqualTo("20260828")
    }

    @Test
    fun preservedForUpload_keepsOnlyRetryableStatuses() {
        assertThat(DailyCallCleanupPolicy.isPreservedForUpload(SyncStatus.PENDING.name)).isTrue()
        assertThat(DailyCallCleanupPolicy.isPreservedForUpload(SyncStatus.UPLOADING.name)).isTrue()
        assertThat(DailyCallCleanupPolicy.isPreservedForUpload("RETRYABLE")).isTrue()

        assertThat(DailyCallCleanupPolicy.isPreservedForUpload(SyncStatus.SYNCED.name)).isFalse()
        assertThat(DailyCallCleanupPolicy.isPreservedForUpload(SyncStatus.FAILED.name)).isFalse()
        assertThat(DailyCallCleanupPolicy.isPreservedForUpload(SyncStatus.NEEDS_REVIEW.name)).isFalse()
    }
}
