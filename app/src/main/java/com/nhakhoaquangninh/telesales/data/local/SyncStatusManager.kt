package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import android.content.SharedPreferences

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

    fun setStatus(fileName: String, status: SyncStatus) {
        prefs.edit().putString(fileName, status.name).apply()
    }

    fun getStatus(fileName: String): SyncStatus {
        val statusStr = prefs.getString(fileName, SyncStatus.PENDING.name)
        return try {
            SyncStatus.valueOf(statusStr ?: SyncStatus.PENDING.name)
        } catch (e: IllegalArgumentException) {
            SyncStatus.PENDING
        }
    }

    fun removeStatus(fileName: String) {
        prefs.edit().remove(fileName).apply()
    }
}
