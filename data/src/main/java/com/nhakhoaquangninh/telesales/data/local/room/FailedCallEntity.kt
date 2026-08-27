package com.nhakhoaquangninh.telesales.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "failed_calls")
data class FailedCallEntity(
    @PrimaryKey val id: String,
    val filePath: String? = null,
    val phoneNumberFrom: String? = null,
    val phoneNumberTo: String? = null,
    val callAtMillis: Long,
    val callAtFormatted: String,
    val callType: String,
    val durationSeconds: Int,
    val callStatus: String,
    val failureReason: String,
    val syncStatus: String,
    val callId: String? = null,
    val ownerUserId: Int = -1
)
