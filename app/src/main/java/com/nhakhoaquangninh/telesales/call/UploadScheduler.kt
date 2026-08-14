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
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata

class UploadScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)
    private val syncStatusManager = SyncStatusManager.getInstance(appContext)

    fun enqueue(metadata: CallRecordMetadata) {
        val recordingId = metadata.recordingUri
        syncStatusManager.setMetadata(recordingId, SyncStatus.PENDING, metadata)
        val input = Data.Builder()
            .putString(UploadAudioWorker.KEY_RECORDING_URI, metadata.recordingUri)
            .putString(UploadAudioWorker.KEY_RECORDING_ID, recordingId)
            .putString(UploadAudioWorker.KEY_PHONE_FROM, metadata.phoneNumberFrom)
            .putString(UploadAudioWorker.KEY_PHONE_TO, metadata.phoneNumberTo)
            .putString(UploadAudioWorker.KEY_CALL_TYPE, metadata.callType.wireValue)
            .putInt(UploadAudioWorker.KEY_DURATION, metadata.durationSeconds)
            .putString(UploadAudioWorker.KEY_CALL_AT, metadata.callAtFormatted)
            .putBoolean(UploadAudioWorker.KEY_IS_ANSWERED, metadata.isAnswered)
            .build()
        val request = OneTimeWorkRequestBuilder<UploadAudioWorker>()
            .setInputData(input)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        workManager.enqueueUniqueWork(
            "Telesales_Upload_${recordingId.hashCode()}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }
}
