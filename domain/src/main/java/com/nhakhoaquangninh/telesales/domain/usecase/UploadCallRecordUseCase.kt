package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.repository.CallRecordRepository

class UploadCallRecordUseCase(private val repository: CallRecordRepository) {
    suspend operator fun invoke(metadata: CallRecordMetadata): Resource<Boolean> {
        if (metadata.isAnswered) {
            val uri = metadata.recordingUri
            if (uri == null || !uri.startsWith("content://")) {
                return Resource.Error(
                    message = "Nguồn tệp ghi âm không hợp lệ",
                    source = ErrorSource.APP_CLIENT
                )
            }
        }
        return repository.uploadCallRecord(metadata)
    }
}