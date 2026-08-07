package com.nhakhoaquangninh.telesales.domain.model

object CallMetadataMapper {
    fun create(
        recordingUri: String,
        callType: CallType,
        otherPhoneNumber: String?,
        ownPhoneNumber: String?,
        durationSeconds: Int,
        callAtFormatted: String?
    ): CallRecordMetadata {
        val otherNumber = otherPhoneNumber.normalizePhoneNumber()
        val ownNumber = ownPhoneNumber.normalizePhoneNumber()
        return CallRecordMetadata(
            recordingUri = recordingUri,
            phoneNumberFrom = if (callType == CallType.INCOMING) otherNumber else ownNumber,
            phoneNumberTo = if (callType == CallType.INCOMING) ownNumber else otherNumber,
            callType = callType,
            durationSeconds = durationSeconds.coerceAtLeast(0),
            callAtFormatted = callAtFormatted
        )
    }

    private fun String?.normalizePhoneNumber(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}