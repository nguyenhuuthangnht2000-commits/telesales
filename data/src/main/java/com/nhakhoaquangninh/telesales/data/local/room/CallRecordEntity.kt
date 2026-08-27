package com.nhakhoaquangninh.telesales.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nhakhoaquangninh.telesales.data.local.SyncStatus

@Entity(tableName = "call_records")
data class CallRecordEntity(
    @PrimaryKey val id: String,
    val status: String = SyncStatus.PENDING.name,
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val recordingUri: String? = null,
    val phoneNumberFrom: String? = null,
    val phoneNumberTo: String? = null,
    val callType: String? = null,
    val durationSeconds: Int = 0,
    val callAtFormatted: String? = null,
    val failureReason: String? = null,
    val isAnswered: Boolean = true,
    val callId: String? = null,
    val ownerUserId: Int = -1,
    val careType: Int? = null,
    val startedAtMillis: Long = 0
)
