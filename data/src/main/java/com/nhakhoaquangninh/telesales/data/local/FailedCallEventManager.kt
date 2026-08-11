package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import androidx.core.content.edit
import com.nhakhoaquangninh.telesales.domain.model.CallType
import com.nhakhoaquangninh.telesales.domain.model.FailureReason
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FailedCallEvent(
    val id: String,
    val filePath: String? = null,
    val phoneNumberFrom: String?,
    val phoneNumberTo: String?,
    val callAtMillis: Long,
    val callAtFormatted: String,
    val callType: CallType = CallType.OUTGOING,
    val durationSeconds: Int = 0,
    val callStatus: String = "failed",
    val failureReason: FailureReason = FailureReason.NOT_CONNECTED,
    val syncStatus: String = "PENDING_SERVER_SUPPORT"
)

class FailedCallEventManager private constructor(context: Context) {
    private val dao = com.nhakhoaquangninh.telesales.data.local.room.TelesalesDatabase.getDatabase(context).failedCallDao()

    companion object {
        @Volatile
        private var instance: FailedCallEventManager? = null

        fun getInstance(context: Context): FailedCallEventManager =
            instance ?: synchronized(this) {
                instance ?: FailedCallEventManager(context.applicationContext).also { instance = it }
            }
    }

    @Synchronized
    fun save(event: FailedCallEvent) {
        dao.insert(event.toEntity())
    }

    @Synchronized
    fun getAll(): List<FailedCallEvent> {
        return dao.getAll().map { it.toEvent() }
    }

    @Synchronized
    fun remove(id: String) {
        dao.delete(id)
    }

    @Synchronized
    fun clearAll() {
        dao.clearAll()
    }

    private fun FailedCallEvent.toEntity() = com.nhakhoaquangninh.telesales.data.local.room.FailedCallEntity(
        id = id,
        filePath = filePath,
        phoneNumberFrom = phoneNumberFrom,
        phoneNumberTo = phoneNumberTo,
        callAtMillis = callAtMillis,
        callAtFormatted = callAtFormatted,
        callType = callType.wireValue,
        durationSeconds = durationSeconds,
        callStatus = callStatus,
        failureReason = failureReason.wireValue,
        syncStatus = syncStatus
    )

    private fun com.nhakhoaquangninh.telesales.data.local.room.FailedCallEntity.toEvent() = FailedCallEvent(
        id = id,
        filePath = filePath,
        phoneNumberFrom = phoneNumberFrom,
        phoneNumberTo = phoneNumberTo,
        callAtMillis = callAtMillis,
        callAtFormatted = callAtFormatted,
        callType = CallType.fromWire(callType) ?: CallType.OUTGOING,
        durationSeconds = durationSeconds,
        callStatus = callStatus,
        failureReason = FailureReason.fromWire(failureReason) ?: FailureReason.NOT_CONNECTED,
        syncStatus = syncStatus
    )
}