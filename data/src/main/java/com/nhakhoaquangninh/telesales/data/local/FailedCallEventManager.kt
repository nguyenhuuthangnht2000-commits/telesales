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
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "failed_call_events"
        private const val KEY_EVENTS = "events"
        private val RETENTION_MILLIS = TimeUnit.DAYS.toMillis(30)

        @Volatile
        private var instance: FailedCallEventManager? = null

        fun getInstance(context: Context): FailedCallEventManager =
            instance ?: synchronized(this) {
                instance ?: FailedCallEventManager(context.applicationContext).also { instance = it }
            }
    }

    @Synchronized
    fun save(event: FailedCallEvent) {
        val root = readRoot()
        root.put(event.id, event.toJson())
        removeExpired(root, System.currentTimeMillis())
        prefs.edit { putString(KEY_EVENTS, root.toString()) }
    }

    @Synchronized
    fun getAll(): List<FailedCallEvent> {
        val root = readRoot()
        if (removeExpired(root, System.currentTimeMillis())) {
            prefs.edit { putString(KEY_EVENTS, root.toString()) }
        }
        return root.keys().asSequence().mapNotNull { key ->
            runCatching { root.getJSONObject(key).toEvent() }.getOrNull()
        }.sortedByDescending { it.callAtMillis }.toList()
    }

    @Synchronized
    fun remove(id: String) {
        val root = readRoot()
        root.remove(id)
        prefs.edit { putString(KEY_EVENTS, root.toString()) }
    }

    private fun removeExpired(root: JSONObject, nowMillis: Long): Boolean {
        val cutoff = nowMillis - RETENTION_MILLIS
        val expiredIds = root.keys().asSequence().filter { key ->
            runCatching { root.getJSONObject(key).optLong("callAtMillis", 0L) }
                .getOrDefault(0L) in 1 until cutoff
        }.toList()
        expiredIds.forEach(root::remove)
        return expiredIds.isNotEmpty()
    }

    private fun readRoot(): JSONObject = runCatching {
        JSONObject(prefs.getString(KEY_EVENTS, null) ?: "{}")
    }.getOrDefault(JSONObject())

    private fun FailedCallEvent.toJson() = JSONObject().apply {
        put("id", id)
        put("filePath", filePath ?: JSONObject.NULL)
        phoneNumberFrom?.let { put("phoneNumberFrom", it) }
        phoneNumberTo?.let { put("phoneNumberTo", it) }
        put("callAtMillis", callAtMillis)
        put("callAtFormatted", callAtFormatted)
        put("callType", callType.wireValue)
        put("durationSeconds", durationSeconds)
        put("callStatus", callStatus)
        put("failureReason", failureReason.wireValue)
        put("syncStatus", syncStatus)
    }

    private fun JSONObject.toEvent() = FailedCallEvent(
        id = getString("id"),
        filePath = optString("filePath").takeIf { it.isNotBlank() && it != "null" },
        phoneNumberFrom = optString("phoneNumberFrom").takeIf { it.isNotBlank() },
        phoneNumberTo = optString("phoneNumberTo").takeIf { it.isNotBlank() },
        callAtMillis = getLong("callAtMillis"),
        callAtFormatted = getString("callAtFormatted"),
        callType = CallType.fromWire(optString("callType")) ?: CallType.OUTGOING,
        durationSeconds = optInt("durationSeconds", 0),
        callStatus = optString("callStatus", "failed"),
        failureReason = FailureReason.fromWire(optString("failureReason")) ?: FailureReason.NOT_CONNECTED,
        syncStatus = optString("syncStatus", "PENDING_SERVER_SUPPORT")
    )
}