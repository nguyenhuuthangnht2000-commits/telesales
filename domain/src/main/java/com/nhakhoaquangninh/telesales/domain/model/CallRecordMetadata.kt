package com.nhakhoaquangninh.telesales.domain.model

data class CallRecordMetadata(
    val filePath: String,
    val phoneNumberFrom: String? = null,
    val phoneNumberTo: String? = null,
    val callType: String? = "outgoing",
    val durationSeconds: Int = 0,
    val callAtFormatted: String? = null // YYYY-MM-DD HH:mm:ss
)
