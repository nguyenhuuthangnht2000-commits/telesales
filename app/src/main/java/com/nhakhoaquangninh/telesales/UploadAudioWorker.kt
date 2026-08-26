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
            ?: "call_${System.currentTimeMillis()}"
        val syncStatusManager = SyncStatusManager.getInstance(applicationContext)
        var terminalStatus = SyncStatus.FAILED
        var failureReason: String? = null
        syncStatusManager.setStatus(recordingId, SyncStatus.UPLOADING)

        val callType = CallType.fromWire(inputData.getString(KEY_CALL_TYPE))
        val duration = inputData.getInt(KEY_DURATION, 0)
        val careType = inputData.getInt(KEY_CARE_TYPE, -1).takeIf { it >= 0 }
        val rawPhoneFrom = inputData.getString(KEY_PHONE_FROM)
        val rawPhoneTo = inputData.getString(KEY_PHONE_TO)
        val metadata = CallRecordMetadata(
            recordingUri = recordingUri,
            phoneNumberFrom = com.nhakhoaquangninh.telesales.call.PhoneNumberNormalizer.normalize(rawPhoneFrom) ?: rawPhoneFrom,
            phoneNumberTo = com.nhakhoaquangninh.telesales.call.PhoneNumberNormalizer.normalize(rawPhoneTo) ?: rawPhoneTo,
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
            FileLogger.setCustomKey("call_care_type", metadata.careType ?: -1)

            var effectiveRecordingUri = recordingUri
            if (isAnswered) {
                val validation = RecordingUriValidator.validate(applicationContext, effectiveRecordingUri)
                if (validation is RecordingUriValidation.Invalid) {
                    val startedAtMillis = inputData.getLong(KEY_STARTED_AT_MILLIS, -1L).takeIf { it > 0 }
                        ?: parseCallAtMillis(metadata.callAtFormatted)
                    val otherPhone = if (metadata.callType == CallType.INCOMING) metadata.phoneNumberFrom else metadata.phoneNumberTo

                    var reMatchedUri: String? = null
                    if (startedAtMillis != null && duration > 0) {
                        val callLogEntry = com.nhakhoaquangninh.telesales.call.CallLogEntry(
                            phoneNumber = otherPhone,
                            callType = metadata.callType,
                            startedAtMillis = startedAtMillis,
                            durationSeconds = duration
                        )
                        val reMatchResult = ServiceLocator.recordingLocator.findMatch(callLogEntry)
                        if (reMatchResult is com.nhakhoaquangninh.telesales.domain.model.RecordingMatchResult.Matched) {
                            val newCandidate = reMatchResult.recording
                            val reValidation = RecordingUriValidator.validate(applicationContext, newCandidate.uri)
                            if (reValidation is RecordingUriValidation.Valid) {
                                effectiveRecordingUri = newCandidate.uri
                                reMatchedUri = newCandidate.uri
                                FileLogger.log(
                                    applicationContext,
                                    "RECORDING_RELOCATED",
                                    "Đã tìm thấy lại file ghi âm sau khi rename/move: ${newCandidate.displayName} (${newCandidate.uri})"
                                )
                            }
                        }
                    }

                    if (reMatchedUri == null) {
                        // File could not be recovered on local storage.
                        // Fallback: Upload metadata without audio file to preserve call records on CRM (Zero Data Loss)
                        FileLogger.logNonFatalError(
                            context = applicationContext,
                            tag = "RECORDING_LOST_FALLBACK",
                            message = "Không tìm thấy file ghi âm (${validation.reason}) cho cuộc gọi tới $otherPhone (${duration}s). Tự động fallback upload metadata.",
                            customKeys = mapOf(
                                "original_uri" to (recordingUri ?: ""),
                                "validation_reason" to validation.reason,
                                "phone" to (otherPhone ?: "")
                            )
                        )
                        effectiveRecordingUri = null
                        ServiceLocator.complianceNotifier.notifyMissingRecording()
                    }
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

            val finalMetadata = metadata.copy(recordingUri = effectiveRecordingUri)
            Log.d("API_LOG", "Bắt đầu Upload File (isAnswered=$isAnswered, careType=${finalMetadata.careType}, uri=${finalMetadata.recordingUri}) - Từ: ${finalMetadata.phoneNumberFrom} | Tới: ${finalMetadata.phoneNumberTo} | Loại: ${finalMetadata.callType} | Thời lượng: ${finalMetadata.durationSeconds}s | Lúc: ${finalMetadata.callAtFormatted}")
            
            val uploadResource = ServiceLocator.uploadCallRecordUseCase(finalMetadata)
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
                            customKeys = mapOf(
                                "http_code" to 401,
                                "server_body" to (uploadResource.rawDetails ?: ""),
                                "care_type" to (metadata.careType ?: -1)
                            )
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
                            customKeys = mapOf(
                                "http_code" to (uploadResource.code ?: -1),
                                "server_body" to (uploadResource.rawDetails ?: ""),
                                "care_type" to (metadata.careType ?: -1)
                            )
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

    private fun parseCallAtMillis(callAt: String?): Long? {
        if (callAt.isNullOrBlank()) return null
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
            }.parse(callAt)?.time
        } catch (_: Exception) {
            null
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
        const val KEY_STARTED_AT_MILLIS = "started_at_millis"
        const val KEY_IS_ANSWERED = "is_answered"
        const val KEY_CARE_TYPE = "care_type"

        private const val TAG = "UploadAudioWorker"
    }
}