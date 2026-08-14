package com.nhakhoaquangninh.telesales

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhakhoaquangninh.telesales.call.RecordingUriValidation
import com.nhakhoaquangninh.telesales.call.RecordingUriValidator
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.SyncStatusManager
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallType
import kotlinx.coroutines.CancellationException

class UploadAudioWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val isAnswered = inputData.getBoolean(KEY_IS_ANSWERED, true)
        val recordingUri = inputData.getString(KEY_RECORDING_URI)
        val recordingId = inputData.getString(KEY_RECORDING_ID)
            ?: recordingUri
            ?: (if (!isAnswered) "missed_${System.currentTimeMillis()}" else return Result.failure())
        val syncStatusManager = SyncStatusManager.getInstance(applicationContext)
        var terminalStatus = SyncStatus.FAILED
        var failureReason: String? = null
        syncStatusManager.setStatus(recordingId, SyncStatus.UPLOADING)

        return try {
            val callType = CallType.fromWire(inputData.getString(KEY_CALL_TYPE))
            val duration = inputData.getInt(KEY_DURATION, 0)
            
            if (isAnswered) {
                val validation = RecordingUriValidator.validate(applicationContext, recordingUri)
                if (validation is RecordingUriValidation.Invalid) {
                    failureReason = validation.reason
                    return@try Result.failure()
                }
                if (callType == null || duration <= 0) {
                    failureReason = "invalid_call_metadata"
                    return@try Result.failure()
                }
            } else {
                if (callType == null) {
                    failureReason = "invalid_call_metadata"
                    return@try Result.failure()
                }
            }

            ServiceLocator.init(applicationContext)
            val metadata = CallRecordMetadata(
                recordingUri = if (isAnswered) requireNotNull(recordingUri) else recordingUri,
                phoneNumberFrom = inputData.getString(KEY_PHONE_FROM),
                phoneNumberTo = inputData.getString(KEY_PHONE_TO),
                callType = callType,
                durationSeconds = duration,
                callAtFormatted = inputData.getString(KEY_CALL_AT),
                isAnswered = isAnswered
            )
            Log.d("API_LOG", "Bắt đầu Upload File (isAnswered=$isAnswered) - Từ: ${metadata.phoneNumberFrom} | Tới: ${metadata.phoneNumberTo} | Loại: ${metadata.callType} | Thời lượng: ${metadata.durationSeconds}s | Lúc: ${metadata.callAtFormatted}")
            val decision = UploadWorkPolicy.decide(
                ServiceLocator.uploadCallRecordUseCase(metadata)
            )
            terminalStatus = decision.terminalStatus
            when (decision.result) {
                UploadWorkResult.SUCCESS -> Result.success()
                UploadWorkResult.RETRY -> Result.retry()
                UploadWorkResult.UNAUTHORIZED -> {
                    UnauthorizedEventBus.notifyUnauthorized()
                    failureReason = "unauthorized"
                    Result.failure()
                }

                UploadWorkResult.FAILURE -> {
                    failureReason = "upload_rejected"
                    Result.failure()
                }
            }
        } catch (cancelled: CancellationException) {
            terminalStatus = SyncStatus.PENDING
            throw cancelled
        } catch (_: SecurityException) {
            failureReason = "recording_permission_denied"
            Result.failure()
        } catch (e: RuntimeException) {
            terminalStatus = SyncStatus.PENDING
            Log.e(TAG, "Tác vụ đồng bộ gặp lỗi tạm thời: ${e.message}", e)
            writeErrorLogToFile(applicationContext, e)
            Result.retry()
        } finally {
            if (failureReason != null) {
                syncStatusManager.setFailure(recordingId, requireNotNull(failureReason))
            } else {
                syncStatusManager.setStatus(recordingId, terminalStatus)
            }
            val intent = android.content.Intent("com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS")
            intent.setPackage(applicationContext.packageName)
            applicationContext.sendBroadcast(intent)
        }
    }

    private fun writeErrorLogToFile(context: Context, e: Exception) {
        try {
            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
            if (dir != null) {
                if (!dir.exists()) dir.mkdirs()
                val file = java.io.File(dir, "telesales_upload_error_log.txt")
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                val logMessage = "[$timestamp] ERROR:\n${Log.getStackTraceString(e)}\n---------------------------\n"
                file.appendText(logMessage)
                Log.d(TAG, "Đã ghi log ra file: ${file.absolutePath}")
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Lỗi khi ghi file log: ${ex.message}")
        }
    }

    companion object {
        const val KEY_RECORDING_URI = "recording_uri"
        const val KEY_RECORDING_ID = "recording_id"
        const val KEY_PHONE_FROM = "phone_from"
        const val KEY_PHONE_TO = "phone_to"
        const val KEY_CALL_TYPE = "call_type"
        const val KEY_DURATION = "duration"
        const val KEY_CALL_AT = "call_at"
        const val KEY_IS_ANSWERED = "is_answered"

        private const val TAG = "UploadAudioWorker"
    }
}