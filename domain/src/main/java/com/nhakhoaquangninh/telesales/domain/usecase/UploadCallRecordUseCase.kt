package com.nhakhoaquangninh.telesales.domain.usecase

import com.nhakhoaquangninh.telesales.domain.common.ErrorSource
import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.repository.CallRecordRepository
import java.io.File

class UploadCallRecordUseCase(private val repository: CallRecordRepository) {

    suspend operator fun invoke(metadata: CallRecordMetadata): Resource<Boolean> {
        val file = File(metadata.filePath)

        if (!file.exists()) {
            return Resource.Error(
                message = "Không tìm thấy file ghi âm tại đường dẫn ${metadata.filePath}",
                source = ErrorSource.APP_CLIENT
            )
        }

        val maxSizeBytes = 50 * 1024 * 1024L
        if (file.length() > maxSizeBytes) {
            return Resource.Error(
                message = "Dung lượng file ghi âm vượt quá 50MB (${file.length() / (1024 * 1024)}MB).",
                source = ErrorSource.APP_CLIENT
            )
        }

        return repository.uploadCallRecord(metadata)
    }
}
