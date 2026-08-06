package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import org.json.JSONObject

enum class SyncStatus {
    PENDING, UPLOADING, SYNCED, FAILED
}

class SyncStatusManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "sync_status_prefs"
        
        @Volatile
        private var instance: SyncStatusManager? = null

        fun getInstance(context: Context): SyncStatusManager {
            return instance ?: synchronized(this) {
                instance ?: SyncStatusManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Tương thích ngược: Nếu chỉ có status thì lưu chuỗi JSON với status
    fun setStatus(fileName: String, status: SyncStatus) {
        val existingMeta = getMetadata(fileName)
        setMetadata(fileName, status, existingMeta)
    }

    fun setMetadata(fileName: String, status: SyncStatus, metadata: CallRecordMetadata?) {
        val json = JSONObject()
        json.put("status", status.name)
        if (metadata != null) {
            json.put("filePath", metadata.filePath)
            metadata.phoneNumberFrom?.let { json.put("phoneNumberFrom", it) }
            metadata.phoneNumberTo?.let { json.put("phoneNumberTo", it) }
            metadata.callType?.let { json.put("callType", it) }
            json.put("durationSeconds", metadata.durationSeconds)
            metadata.callAtFormatted?.let { json.put("callAtFormatted", it) }
        }
        prefs.edit { putString(fileName, json.toString()) }
    }

    fun getStatus(fileName: String): SyncStatus {
        val str = prefs.getString(fileName, null) ?: return SyncStatus.PENDING
        if (str.startsWith("{")) {
            return try {
                val json = JSONObject(str)
                SyncStatus.valueOf(json.optString("status", SyncStatus.PENDING.name))
            } catch (e: Exception) {
                SyncStatus.PENDING
            }
        }
        // Tương thích ngược với chuỗi enum cũ
        return try {
            SyncStatus.valueOf(str)
        } catch (e: IllegalArgumentException) {
            SyncStatus.PENDING
        }
    }

    fun getMetadata(fileName: String): CallRecordMetadata? {
        val str = prefs.getString(fileName, null) ?: return null
        if (!str.startsWith("{")) return null
        
        return try {
            val json = JSONObject(str)
            CallRecordMetadata(
                filePath = json.optString("filePath", ""),
                phoneNumberFrom = if (json.has("phoneNumberFrom")) json.getString("phoneNumberFrom") else null,
                phoneNumberTo = if (json.has("phoneNumberTo")) json.getString("phoneNumberTo") else null,
                callType = if (json.has("callType")) json.getString("callType") else null,
                durationSeconds = json.optInt("durationSeconds", 0),
                callAtFormatted = if (json.has("callAtFormatted")) json.getString("callAtFormatted") else null
            )
        } catch (e: Exception) {
            null
        }
    }

    fun removeStatus(fileName: String) {
        prefs.edit { remove(fileName) }
    }
}
