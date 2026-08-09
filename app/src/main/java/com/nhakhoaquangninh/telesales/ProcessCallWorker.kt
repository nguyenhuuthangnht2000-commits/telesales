package com.nhakhoaquangninh.telesales

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhakhoaquangninh.telesales.call.CallSessionSnapshot

class ProcessCallWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val startedAt = inputData.getLong(KEY_STARTED_AT, 0L)
        val endedAt = inputData.getLong(KEY_ENDED_AT, 0L)
        if (startedAt !in 1..endedAt) return Result.failure()
        ServiceLocator.init(applicationContext)
        val snapshot = CallSessionSnapshot(
            sessionId = inputData.getLong(KEY_SESSION_ID, 0L),
            incoming = inputData.getBoolean(KEY_INCOMING, false),
            otherPhoneNumber = inputData.getString(KEY_OTHER_PHONE),
            startedAtMillis = startedAt,
            endedAtMillis = endedAt
        )
        return runCatching {
            ServiceLocator.callEventCoordinator.process(
                snapshot,
                inputData.getBoolean(KEY_MISSED_INCOMING, false)
            )
            val intent = android.content.Intent("com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS")
            intent.setPackage(applicationContext.packageName)
            applicationContext.sendBroadcast(intent)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val KEY_SESSION_ID = "session_id"
        const val KEY_INCOMING = "incoming"
        const val KEY_OTHER_PHONE = "other_phone"
        const val KEY_STARTED_AT = "started_at"
        const val KEY_ENDED_AT = "ended_at"
        const val KEY_MISSED_INCOMING = "missed_incoming"
    }
}
