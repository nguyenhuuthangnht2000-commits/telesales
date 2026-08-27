package com.nhakhoaquangninh.telesales

import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object DailyCallCleanupPolicy {
    private val vietnamTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")

    fun startOfCurrentDayMillis(nowMillis: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance(vietnamTimeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun nextCleanupDelayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        val nextMidnight = Calendar.getInstance(vietnamTimeZone).apply {
            timeInMillis = startOfCurrentDayMillis(nowMillis)
            add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        return nextMidnight - nowMillis
    }

    fun cleanupDayKey(nowMillis: Long = System.currentTimeMillis()): String =
        SimpleDateFormat("yyyyMMdd", Locale.US).apply {
            timeZone = vietnamTimeZone
        }.format(nowMillis)

    fun isPreservedForUpload(status: String): Boolean = status in PRESERVED_UPLOAD_STATUSES

    private val PRESERVED_UPLOAD_STATUSES = setOf("PENDING", "UPLOADING", "RETRYABLE")
}
