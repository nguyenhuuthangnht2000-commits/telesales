package com.nhakhoaquangninh.telesales.call

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nhakhoaquangninh.telesales.UploadAudioWorker
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.SyncStatusManager
import com.nhakhoaquangninh.telesales.data.local.TokenManager
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata

class UploadScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val syncStatusManager = SyncStatusManager.getInstance(appContext)

    fun enqueue(metadata: CallRecordMetadata) {
        val recordingId = metadata.recordingUri ?: "missed_${System.currentTimeMillis()}"
        val effectiveCareType = metadata.careType
            ?: TokenManager.getInstance(appContext).getSelectedCareTypeValue()
        val enrichedMetadata = if (metadata.careType == null && effectiveCareType != null) {
            metadata.copy(careType = effectiveCareType)
        } else {
            metadata
        }
        syncStatusManager.setMetadata(recordingId, SyncStatus.PENDING, enrichedMetadata)
        val input = Data.Builder()
            .putString(UploadAudioWorker.KEY_RECORDING_URI, enrichedMetadata.recordingUri)
            .putString(UploadAudioWorker.KEY_RECORDING_ID, recordingId)
            .putString(UploadAudioWorker.KEY_PHONE_FROM, enrichedMetadata.phoneNumberFrom)
            .putString(UploadAudioWorker.KEY_PHONE_TO, enrichedMetadata.phoneNumberTo)
            .putString(UploadAudioWorker.KEY_CALL_TYPE, enrichedMetadata.callType.wireValue)
            .putInt(UploadAudioWorker.KEY_DURATION, enrichedMetadata.durationSeconds)
            .putString(UploadAudioWorker.KEY_CALL_AT, enrichedMetadata.callAtFormatted)
            .putBoolean(UploadAudioWorker.KEY_IS_ANSWERED, enrichedMetadata.isAnswered)
            .apply {
                if (effectiveCareType != null) {
                    putInt(UploadAudioWorker.KEY_CARE_TYPE, effectiveCareType)
                }
            }
            .build()
        val request = OneTimeWorkRequestBuilder<UploadAudioWorker>()
            .setInputData(input)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                15_000L,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        workManager.enqueueUniqueWork(
            "Telesales_Upload_${recordingId.hashCode()}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
