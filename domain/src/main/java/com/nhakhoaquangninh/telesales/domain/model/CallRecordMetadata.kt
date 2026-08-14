package com.nhakhoaquangninh.telesales.domain.model

data class CallRecordMetadata(
    val recordingUri: String?,
    val phoneNumberFrom: String? = null,
    val phoneNumberTo: String? = null,
    val callType: CallType = CallType.OUTGOING,
    val durationSeconds: Int = 0,
    val callAtFormatted: String? = null,
    val isAnswered: Boolean = true
)