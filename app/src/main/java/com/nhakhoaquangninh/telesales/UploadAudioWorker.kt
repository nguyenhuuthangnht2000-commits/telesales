package com.nhakhoaquangninh.telesales

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nhakhoaquangninh.telesales.call.RecordingUriValidation
import com.nhakhoaquangninh.telesales.call.RecordingUriValidator
import com.nhakhoaquangninh.telesales.core.FileLogger
import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.data.local.SyncStatusManager
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallType
import kotlinx.coroutines.CancellationException

class UploadAudioWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        ServiceLocator.init(applicationContext)
        val isAnswered = inputData.getBoolean(KEY_IS_ANSWERED, true)
        val recordingUri = inputData.getString(KEY_RECORDING_URI)
        val recordingId = inputData.getString(KEY_RECORDING_ID)
            ?: recordingUri
            ?: (if (!isAnswered) "missed_${System.currentTimeMillis()}" else {
                FileLogger.log(applicationContext, "WORKER_ERROR", "Thiếu ID hoặc URI ghi âm khi bắt đầu worker.")
                return Result.failure()
            })
        val syncStatusManager = SyncStatusManager.getInstance(applicationContext)
        var terminalStatus = SyncStatus.FAILED
        var failureReason: String? = null
        syncStatusManager.setStatus(recordingId, SyncStatus.UPLOADING)

        val callType = CallType.fromWire(inputData.getString(KEY_CALL_TYPE))
        val duration = inputData.getInt(KEY_DURATION, 0)
        val careType = inputData.getInt(KEY_CARE_TYPE, -1).takeIf { it >= 0 }
        val metadata = CallRecordMetadata(
            recordingUri = if (isAnswered) (recordingUri ?: "") else recordingUri,
            phoneNumberFrom = inputData.getString(KEY_PHONE_FROM),
            phoneNumberTo = inputData.getString(KEY_PHONE_TO),
            callType = callType ?: CallType.OUTGOING,
            durationSeconds = duration,
            callAtFormatted = inputData.getString(KEY_CALL_AT),
            isAnswered = isAnswered,
            careType = careType
        )

        return try {
            FileLogger.setCustomKey("call_phone_from", metadata.phoneNumberFrom ?: "")
            FileLogger.setCustomKey("call_phone_to", metadata.phoneNumberTo ?: "")
            FileLogger.setCustomKey("call_type", metadata.callType.wireValue)
            FileLogger.setCustomKey("call_duration", metadata.durationSeconds)
            FileLogger.setCustomKey("call_is_answered", metadata.isAnswered)

            if (isAnswered) {
                val validation = RecordingUriValidator.validate(applicationContext, recordingUri)
                if (validation is RecordingUriValidation.Invalid) {
                    failureReason = validation.reason
                    FileLogger.logNonFatalError(
                        context = applicationContext,
                        tag = "VALIDATION_ERROR",
                        message = "Tệp ghi âm không hợp lệ ($failureReason) - URI: $recordingUri",
                        customKeys = mapOf("validation_reason" to failureReason, "uri" to (recordingUri ?: ""))
                    )
                    ServiceLocator.complianceNotifier.notifyUploadFailed(metadata, failureReason)
                    return Result.failure()
                }
                if (callType == null || duration <= 0) {
                    failureReason = "invalid_call_metadata"
                    FileLogger.logNonFatalError(
                        context = applicationContext,
                        tag = "METADATA_ERROR",
                        message = "Metadata không hợp lệ (callType=$callType, duration=$duration) - URI: $recordingUri",
                        customKeys = mapOf("callType" to (callType?.wireValue ?: "null"), "duration" to duration)
                    )
                    ServiceLocator.complianceNotifier.notifyUploadFailed(metadata, failureReason)
                    return Result.failure()
                }
            } else {
                if (callType == null) {
                    failureReason = "invalid_call_metadata"
                    FileLogger.logNonFatalError(
                        context = applicationContext,
                        tag = "METADATA_ERROR",
                        message = "Metadata cuộc gọi không hợp lệ (callType=null) - ID: $recordingId",
                        customKeys = mapOf("recordingId" to recordingId)
                    )
                    ServiceLocator.complianceNotifier.notifyUploadFailed(metadata, failureReason)
                    return Result.failure()
                }
            }

            Log.d("API_LOG", "Bắt đầu Upload File (isAnswered=$isAnswered) - Từ: ${metadata.phoneNumberFrom} | Tới: ${metadata.phoneNumberTo} | Loại: ${metadata.callType} | Thời lượng: ${metadata.durationSeconds}s | Lúc: ${metadata.callAtFormatted}")
            
            val uploadResource = ServiceLocator.uploadCallRecordUseCase(metadata)
            val decision = UploadWorkPolicy.decide(uploadResource)
            terminalStatus = decision.terminalStatus
            when (decision.result) {
                UploadWorkResult.SUCCESS -> {
                    ServiceLocator.complianceNotifier.notifyUploadSuccess(metadata)
                    Result.success()
                }
                UploadWorkResult.RETRY -> {
                    if (uploadResource is Resource.Error) {
                        FileLogger.log(applicationContext, "WORKER_RETRY", "Upload cần retry (HTTP ${uploadResource.code}): ${uploadResource.message} | Server body: ${uploadResource.rawDetails}")
                    }
                    Result.retry()
                }
                UploadWorkResult.UNAUTHORIZED -> {
                    if (uploadResource is Resource.Error) {
                        FileLogger.logNonFatalError(
                            context = applicationContext,
                            tag = "WORKER_UNAUTHORIZED",
                            message = "Upload bị từ chối xác thực (HTTP 401): ${uploadResource.message} | Server body: ${uploadResource.rawDetails}",
                            customKeys = mapOf("http_code" to 401, "server_body" to (uploadResource.rawDetails ?: ""))
                        )
                    }
                    UnauthorizedEventBus.notifyUnauthorized()
                    failureReason = "unauthorized"
                    ServiceLocator.complianceNotifier.notifyUploadFailed(metadata, failureReason)
                    Result.failure()
                }

                UploadWorkResult.FAILURE -> {
                    if (uploadResource is Resource.Error) {
                        FileLogger.logNonFatalError(
                            context = applicationContext,
                            tag = "WORKER_REJECTED",
                            message = "Upload bị Server từ chối (HTTP ${uploadResource.code}): ${uploadResource.message} | Server body: ${uploadResource.rawDetails}",
                            customKeys = mapOf("http_code" to (uploadResource.code ?: -1), "server_body" to (uploadResource.rawDetails ?: ""))
                        )
                    }
                    failureReason = "upload_rejected"
                    ServiceLocator.complianceNotifier.notifyUploadFailed(metadata, failureReason)
                    Result.failure()
                }
            }
        } catch (cancelled: CancellationException) {
            terminalStatus = SyncStatus.PENDING
            FileLogger.log(applicationContext, "WORKER_CANCELLED", "Worker bị huỷ bỏ (CancellationException)")
            throw cancelled
        } catch (se: SecurityException) {
            failureReason = "recording_permission_denied"
            FileLogger.logException(applicationContext, "PERMISSION_DENIED", "Thiếu quyền truy cập file/gọi", se)
            ServiceLocator.complianceNotifier.notifyUploadFailed(metadata, failureReason)
            Result.failure()
        } catch (e: RuntimeException) {
            terminalStatus = SyncStatus.PENDING
            Log.e(TAG, "Tác vụ đồng bộ gặp lỗi tạm thời: ${e.message}", e)
            FileLogger.logException(applicationContext, "RUNTIME_EXCEPTION", "Tác vụ đồng bộ gặp lỗi RuntimeException: ${e.message}", e)
            Result.retry()
        } finally {
            if (failureReason != null) {
                syncStatusManager.setFailure(recordingId, failureReason)
            } else {
                syncStatusManager.setStatus(recordingId, terminalStatus)
            }
            val intent = android.content.Intent("com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS")
            intent.setPackage(applicationContext.packageName)
            applicationContext.sendBroadcast(intent)
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
        const val KEY_CARE_TYPE = "care_type"

        private const val TAG = "UploadAudioWorker"
    }
}