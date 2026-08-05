package com.nhakhoaquangninh.telesales

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.SyncStatusManager
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import java.io.File

class UploadAudioWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_FILE_PATH = "file_path"
        const val KEY_PHONE_FROM = "phone_from"
        const val KEY_PHONE_TO = "phone_to"
        const val KEY_CALL_TYPE = "call_type"
        const val KEY_DURATION = "duration"
        const val KEY_CALL_AT = "call_at"

        private const val TAG = "UploadAudioWorker"
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH)

        if (filePath.isNullOrEmpty()) {
            Log.e(TAG, "File path is empty")
            return Result.failure()
        }

        // Ensure ServiceLocator is initialized (Worker may run without Application)
        ServiceLocator.init(applicationContext)

        val uploadUseCase = ServiceLocator.uploadCallRecordUseCase

        val metadata = CallRecordMetadata(
            filePath = filePath,
            phoneNumberFrom = inputData.getString(KEY_PHONE_FROM),
            phoneNumberTo = inputData.getString(KEY_PHONE_TO),
            callType = inputData.getString(KEY_CALL_TYPE) ?: "outgoing",
            durationSeconds = inputData.getInt(KEY_DURATION, 0),
            callAtFormatted = inputData.getString(KEY_CALL_AT)
        )

        Log.d(TAG, "🚀 Bắt đầu upload file: $filePath")
        val fileName = File(filePath).name
        val syncStatusManager = SyncStatusManager.getInstance(applicationContext)
        syncStatusManager.setStatus(fileName, SyncStatus.UPLOADING)

        return when (val result = uploadUseCase(metadata)) {
            is Resource.Success -> {
                Log.d(TAG, "✅ Upload thành công: ${result.message}")
                syncStatusManager.setStatus(fileName, SyncStatus.SYNCED)
                Result.success()
            }

            is Resource.Error -> {
                Log.e(TAG, "❌ Upload thất bại: ${result.message}")
                syncStatusManager.setStatus(fileName, SyncStatus.FAILED)
                if (result.code in 500..599 || result.source == ErrorSource.NETWORK) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }

            else -> {
                syncStatusManager.setStatus(fileName, SyncStatus.FAILED)
                Result.failure()
            }
        }
    }
}
