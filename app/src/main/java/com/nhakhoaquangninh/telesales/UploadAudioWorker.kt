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
        val recordingUri = inputData.getString(KEY_RECORDING_URI)
        val recordingId = inputData.getString(KEY_RECORDING_ID)
            ?: recordingUri
            ?: return Result.failure()
        val syncStatusManager = SyncStatusManager.getInstance(applicationContext)
        var terminalStatus = SyncStatus.FAILED
        var failureReason: String? = null
        syncStatusManager.setStatus(recordingId, SyncStatus.UPLOADING)

        return try {
            when (val validation =
                RecordingUriValidator.validate(applicationContext, recordingUri)) {
                is RecordingUriValidation.Invalid -> {
                    failureReason = validation.reason
                    Result.failure()
                }

                is RecordingUriValidation.Valid -> {
                    val callType = CallType.fromWire(inputData.getString(KEY_CALL_TYPE))
                    val duration = inputData.getInt(KEY_DURATION, 0)
                    if (callType == null || duration <= 0) {
                        failureReason = "invalid_call_metadata"
                        Result.failure()
                    } else {
                        ServiceLocator.init(applicationContext)
                        val metadata = CallRecordMetadata(
                            recordingUri = requireNotNull(recordingUri),
                            phoneNumberFrom = inputData.getString(KEY_PHONE_FROM),
                            phoneNumberTo = inputData.getString(KEY_PHONE_TO),
                            callType = callType,
                            durationSeconds = duration,
                            callAtFormatted = inputData.getString(KEY_CALL_AT)
                        )
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
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            terminalStatus = SyncStatus.PENDING
            throw cancelled
        } catch (_: SecurityException) {
            failureReason = "recording_permission_denied"
            Result.failure()
        } catch (_: RuntimeException) {
            terminalStatus = SyncStatus.PENDING
            Log.e(TAG, "Tác vụ đồng bộ gặp lỗi tạm thời")
            Result.retry()
        } finally {
            if (failureReason != null) {
                syncStatusManager.setFailure(recordingId, requireNotNull(failureReason))
            } else {
                syncStatusManager.setStatus(recordingId, terminalStatus)
            }
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

        private const val TAG = "UploadAudioWorker"
    }
}