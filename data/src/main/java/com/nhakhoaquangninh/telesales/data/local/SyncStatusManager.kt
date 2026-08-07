package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallType
import java.util.concurrent.TimeUnit
import org.json.JSONObject

enum class SyncStatus {
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED,
    NEEDS_REVIEW
}

class SyncStatusManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "sync_status_prefs"
        private const val KEY_UPDATED_AT_MILLIS = "updatedAtMillis"
        private const val KEY_RECORDING_URI = "recordingUri"
        private const val KEY_LEGACY_FILE_PATH = "filePath"
        private const val KEY_FAILURE_REASON = "failureReason"
        private val RETENTION_MILLIS = TimeUnit.DAYS.toMillis(30)

        @Volatile
        private var instance: SyncStatusManager? = null

        fun getInstance(context: Context): SyncStatusManager =
            instance ?: synchronized(this) {
                instance ?: SyncStatusManager(context.applicationContext).also { instance = it }
            }
    }

    fun setStatus(recordingId: String, status: SyncStatus) {
        setMetadata(recordingId, status, getMetadata(recordingId))
    }

    fun setFailure(recordingId: String, reason: String) {
        val metadata = getMetadata(recordingId)
        val json = createJson(SyncStatus.FAILED, metadata)
            .put(KEY_FAILURE_REASON, reason)
        prefs.edit { putString(recordingId, json.toString()) }
    }

    fun setMetadata(
        recordingId: String,
        status: SyncStatus,
        metadata: CallRecordMetadata?
    ) {
        prefs.edit {
            putString(recordingId, createJson(status, metadata).toString())
        }
    }

    fun getStatus(recordingId: String): SyncStatus {
        val value = getUnexpiredValue(recordingId) ?: return SyncStatus.PENDING
        if (value.startsWith("{")) {
            return runCatching {
                SyncStatus.valueOf(
                    JSONObject(value).optString("status", SyncStatus.PENDING.name)
                )
            }.getOrDefault(SyncStatus.PENDING)
        }
        return runCatching { SyncStatus.valueOf(value) }.getOrDefault(SyncStatus.PENDING)
    }

    fun getMetadata(recordingId: String): CallRecordMetadata? {
        val value = getUnexpiredValue(recordingId) ?: return null
        if (!value.startsWith("{")) return null
        return runCatching {
            val json = JSONObject(value)
            val callType = CallType.fromWire(json.optString("callType"))
                ?: return@runCatching null
            val recordingUri = json.optString(KEY_RECORDING_URI)
                .takeIf(String::isNotBlank)
                ?: json.optString(KEY_LEGACY_FILE_PATH).takeIf(String::isNotBlank)
                ?: return@runCatching null
            CallRecordMetadata(
                recordingUri = recordingUri,
                phoneNumberFrom = json.optString("phoneNumberFrom").takeIf(String::isNotBlank),
                phoneNumberTo = json.optString("phoneNumberTo").takeIf(String::isNotBlank),
                callType = callType,
                durationSeconds = json.optInt("durationSeconds", 0),
                callAtFormatted = json.optString("callAtFormatted").takeIf(String::isNotBlank)
            )
        }.getOrNull()
    }

    fun getFailureReason(recordingId: String): String? {
        val value = getUnexpiredValue(recordingId) ?: return null
        return runCatching {
            JSONObject(value).optString(KEY_FAILURE_REASON).takeIf(String::isNotBlank)
        }.getOrNull()
    }

    fun removeStatus(recordingId: String) {
        prefs.edit { remove(recordingId) }
    }

    private fun createJson(
        status: SyncStatus,
        metadata: CallRecordMetadata?
    ): JSONObject = JSONObject()
        .put("status", status.name)
        .put(KEY_UPDATED_AT_MILLIS, System.currentTimeMillis())
        .apply {
            if (metadata != null) {
                put(KEY_RECORDING_URI, metadata.recordingUri)
                metadata.phoneNumberFrom?.let { put("phoneNumberFrom", it) }
                metadata.phoneNumberTo?.let { put("phoneNumberTo", it) }
                put("callType", metadata.callType.wireValue)
                put("durationSeconds", metadata.durationSeconds)
                metadata.callAtFormatted?.let { put("callAtFormatted", it) }
            }
        }

    private fun getUnexpiredValue(recordingId: String): String? {
        val value = prefs.getString(recordingId, null) ?: return null
        if (!value.startsWith("{")) return value
        val json = runCatching { JSONObject(value) }.getOrNull()
        if (json == null) {
            prefs.edit { remove(recordingId) }
            return null
        }
        val updatedAt = json.optLong(KEY_UPDATED_AT_MILLIS, 0L)
        if (updatedAt <= 0L) {
            val migrated = json.put(KEY_UPDATED_AT_MILLIS, System.currentTimeMillis()).toString()
            prefs.edit { putString(recordingId, migrated) }
            return migrated
        }
        if (updatedAt < System.currentTimeMillis() - RETENTION_MILLIS) {
            prefs.edit { remove(recordingId) }
            return null
        }
        return value
    }
}