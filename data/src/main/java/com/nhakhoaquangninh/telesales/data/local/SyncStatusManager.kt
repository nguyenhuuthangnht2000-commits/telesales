package com.nhakhoaquangninh.telesales.data.local

import android.content.Context
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallType

enum class SyncStatus {
    PENDING,
    UPLOADING,
    SYNCED,
    FAILED,
    NEEDS_REVIEW
}

class SyncStatusManager private constructor(context: Context) {
    private val dao =
        com.nhakhoaquangninh.telesales.data.local.room.TelesalesDatabase.getDatabase(context)
            .callRecordDao()

    companion object {
        @Volatile
        private var instance: SyncStatusManager? = null

        fun getInstance(context: Context): SyncStatusManager =
            instance ?: synchronized(this) {
                instance ?: SyncStatusManager(context.applicationContext).also { instance = it }
            }
    }

    @Synchronized
    fun setStatus(recordingId: String, status: SyncStatus) {
        val existing = dao.getById(recordingId)
        val entity = existing?.copy(
            status = status.name,
            updatedAtMillis = System.currentTimeMillis()
        ) ?: com.nhakhoaquangninh.telesales.data.local.room.CallRecordEntity(
            id = recordingId,
            status = status.name,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.insert(entity)
    }

    @Synchronized
    fun setFailure(recordingId: String, reason: String) {
        val existing = dao.getById(recordingId)
        val entity = existing?.copy(
            status = SyncStatus.FAILED.name,
            failureReason = reason,
            updatedAtMillis = System.currentTimeMillis()
        ) ?: com.nhakhoaquangninh.telesales.data.local.room.CallRecordEntity(
            id = recordingId,
            status = SyncStatus.FAILED.name,
            failureReason = reason,
            updatedAtMillis = System.currentTimeMillis()
        )
        dao.insert(entity)
    }

    @Synchronized
    fun setMetadata(
        recordingId: String,
        status: SyncStatus,
        metadata: CallRecordMetadata?
    ) {
        val existing = dao.getById(recordingId)
        val entity = com.nhakhoaquangninh.telesales.data.local.room.CallRecordEntity(
            id = recordingId,
            status = status.name,
            updatedAtMillis = System.currentTimeMillis(),
            recordingUri = metadata?.recordingUri ?: existing?.recordingUri,
            phoneNumberFrom = metadata?.phoneNumberFrom ?: existing?.phoneNumberFrom,
            phoneNumberTo = metadata?.phoneNumberTo ?: existing?.phoneNumberTo,
            callType = metadata?.callType?.wireValue ?: existing?.callType,
            durationSeconds = metadata?.durationSeconds ?: existing?.durationSeconds ?: 0,
            callAtFormatted = metadata?.callAtFormatted ?: existing?.callAtFormatted,
            failureReason = existing?.failureReason,
            isAnswered = metadata?.isAnswered ?: existing?.isAnswered ?: true,
            callId = metadata?.callId ?: existing?.callId,
            ownerUserId = metadata?.ownerUserId ?: existing?.ownerUserId ?: -1,
            careType = metadata?.careType ?: existing?.careType,
            startedAtMillis = metadata?.startedAtMillis ?: existing?.startedAtMillis ?: 0L
        )
        dao.insert(entity)
    }

    @Synchronized
    fun getStatus(recordingId: String): SyncStatus {
        val entity = dao.getById(recordingId) ?: return SyncStatus.PENDING
        return runCatching { SyncStatus.valueOf(entity.status) }.getOrDefault(SyncStatus.PENDING)
    }

    @Synchronized
    fun getMetadata(recordingId: String): CallRecordMetadata? {
        val entity = dao.getById(recordingId) ?: return null
        val callTypeStr = entity.callType ?: return null
        val callType = CallType.fromWire(callTypeStr) ?: return null
        val recordingUri = entity.recordingUri.takeIf { !it.isNullOrBlank() }
        if (entity.isAnswered && recordingUri == null) return null
        val callId = entity.callId ?: return null

        return CallRecordMetadata(
            callId = callId,
            ownerUserId = entity.ownerUserId,
            startedAtMillis = entity.startedAtMillis,
            recordingUri = recordingUri,
            phoneNumberFrom = entity.phoneNumberFrom,
            phoneNumberTo = entity.phoneNumberTo,
            callType = callType,
            durationSeconds = entity.durationSeconds,
            callAtFormatted = entity.callAtFormatted,
            isAnswered = entity.isAnswered,
            careType = entity.careType
        )
    }

    @Synchronized
    fun getFailureReason(recordingId: String): String? {
        return dao.getById(recordingId)?.failureReason
    }

    @Synchronized
    fun removeStatus(recordingId: String) {
        dao.delete(recordingId)
    }
}