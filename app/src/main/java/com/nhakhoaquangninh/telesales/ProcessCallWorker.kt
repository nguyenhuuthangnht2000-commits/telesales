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
            endedAtMillis = endedAt,
            ownerUserId = inputData.getInt(KEY_OWNER_USER_ID, -1),
            ownPhoneNumber = inputData.getString(KEY_OWN_PHONE_NUMBER),
            careType = if (inputData.keyValueMap.containsKey(KEY_CARE_TYPE)) inputData.getInt(KEY_CARE_TYPE, -1) else null,
            answered = inputData.getBoolean(KEY_ANSWERED, false)
        )
        return try {
            ServiceLocator.callEventCoordinator.process(
                snapshot,
                inputData.getBoolean(KEY_MISSED_INCOMING, false)
            )
            val intent = android.content.Intent("com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS")
            intent.setPackage(applicationContext.packageName)
            applicationContext.sendBroadcast(intent)
            Result.success()
        } catch (ce: kotlinx.coroutines.CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_SESSION_ID = "session_id"
        const val KEY_INCOMING = "incoming"
        const val KEY_OTHER_PHONE = "other_phone"
        const val KEY_STARTED_AT = "started_at"
        const val KEY_ENDED_AT = "ended_at"
        const val KEY_MISSED_INCOMING = "missed_incoming"
        const val KEY_OWNER_USER_ID = "owner_user_id"
        const val KEY_OWN_PHONE_NUMBER = "own_phone_number"
        const val KEY_CARE_TYPE = "care_type"
        const val KEY_ANSWERED = "answered"
    }
}
