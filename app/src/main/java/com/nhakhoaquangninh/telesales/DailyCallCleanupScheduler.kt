package com.nhakhoaquangninh.telesales

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DailyCallCleanupScheduler {
    private const val WORK_NAME_PREFIX = "Telesales_DailyCallCleanup_"
    private const val PREFERENCES_NAME = "daily_call_cleanup"
    private const val KEY_LAST_CLEANUP_DAY = "last_cleanup_day"

    fun schedule(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val appContext = context.applicationContext
        if (!hasCompletedToday(appContext, nowMillis)) {
            enqueueCleanup(appContext, nowMillis, initialDelayMillis = 0L)
        }
        scheduleNext(appContext, nowMillis)
    }

    fun scheduleNext(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        val delayMillis = DailyCallCleanupPolicy.nextCleanupDelayMillis(nowMillis)
        enqueueCleanup(context.applicationContext, nowMillis + delayMillis, delayMillis)
    }

    fun markCompleted(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        context.applicationContext
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_CLEANUP_DAY, DailyCallCleanupPolicy.cleanupDayKey(nowMillis))
            .apply()
    }

    private fun hasCompletedToday(context: Context, nowMillis: Long): Boolean =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_CLEANUP_DAY, null) == DailyCallCleanupPolicy.cleanupDayKey(nowMillis)

    private fun enqueueCleanup(context: Context, targetMillis: Long, initialDelayMillis: Long) {
        val targetDate = DailyCallCleanupPolicy.cleanupDayKey(targetMillis)
        val request = OneTimeWorkRequestBuilder<DailyCallCleanupWorker>()
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME_PREFIX + targetDate,
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
