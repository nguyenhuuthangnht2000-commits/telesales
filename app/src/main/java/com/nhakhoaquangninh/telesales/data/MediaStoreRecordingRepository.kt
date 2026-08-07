package com.nhakhoaquangninh.telesales.data

import com.nhakhoaquangninh.telesales.call.RecordingLocator
import com.nhakhoaquangninh.telesales.domain.model.RecordingCandidate
import com.nhakhoaquangninh.telesales.domain.repository.RecordingRepository

class MediaStoreRecordingRepository(
    private val locator: RecordingLocator
) : RecordingRepository {
    override suspend fun getApprovedRecordings(): List<RecordingCandidate> =
        locator.getApprovedRecordings()
}
