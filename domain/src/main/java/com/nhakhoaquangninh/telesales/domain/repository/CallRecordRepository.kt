package com.nhakhoaquangninh.telesales.domain.repository

import com.nhakhoaquangninh.telesales.domain.common.Resource
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata

interface CallRecordRepository {
    suspend fun uploadCallRecord(metadata: CallRecordMetadata): Resource<Boolean>
}
