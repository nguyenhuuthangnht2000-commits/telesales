package com.nhakhoaquangninh.telesales

import com.nhakhoaquangninh.telesales.data.local.SyncStatus
import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource

enum class UploadWorkResult { SUCCESS, RETRY, FAILURE, UNAUTHORIZED }

data class UploadWorkDecision(
    val result: UploadWorkResult,
    val terminalStatus: SyncStatus
)

object UploadWorkPolicy {
    fun decide(resource: Resource<Boolean>): UploadWorkDecision = when (resource) {
        is Resource.Success -> UploadWorkDecision(UploadWorkResult.SUCCESS, SyncStatus.SYNCED)
        is Resource.Error -> when {
            resource.code == 401 ->
                UploadWorkDecision(UploadWorkResult.UNAUTHORIZED, SyncStatus.FAILED)

            resource.source == ErrorSource.NETWORK || resource.code in 500..599 ->
                UploadWorkDecision(UploadWorkResult.RETRY, SyncStatus.PENDING)

            else -> UploadWorkDecision(UploadWorkResult.FAILURE, SyncStatus.FAILED)
        }

        else -> UploadWorkDecision(UploadWorkResult.FAILURE, SyncStatus.FAILED)
    }
}
