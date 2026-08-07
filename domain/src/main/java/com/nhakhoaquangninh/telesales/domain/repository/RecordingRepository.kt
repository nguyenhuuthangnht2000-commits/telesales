package com.nhakhoaquangninh.telesales.domain.repository

import com.nhakhoaquangninh.telesales.domain.model.RecordingCandidate

interface RecordingRepository {
    suspend fun getApprovedRecordings(): List<RecordingCandidate>
}
