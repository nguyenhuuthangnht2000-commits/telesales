package com.nhakhoaquangninh.telesales.domain.model

object CallMetadataMapper {
    fun applyOwnPhoneNumber(
        metadata: CallRecordMetadata,
        ownPhoneNumber: String?
    ): CallRecordMetadata {
        val ownNumber = ownPhoneNumber.normalizePhoneNumber() ?: return metadata
        return if (metadata.callType == CallType.INCOMING) {
            metadata.copy(phoneNumberTo = ownNumber)
        } else {
            metadata.copy(phoneNumberFrom = ownNumber)
        }
    }

    fun hasOwnPhoneNumber(metadata: CallRecordMetadata): Boolean =
        if (metadata.callType == CallType.INCOMING) {
            !metadata.phoneNumberTo.isNullOrBlank()
        } else {
            !metadata.phoneNumberFrom.isNullOrBlank()
        }

    fun create(
        ownerUserId: Int,
        startedAtMillis: Long,
        recordingUri: String?,
        callType: CallType,
        otherPhoneNumber: String?,
        ownPhoneNumber: String?,
        durationSeconds: Int,
        callAtFormatted: String?,
        isAnswered: Boolean = true,
        careType: Int? = null
    ): CallRecordMetadata {
        val otherNumber = otherPhoneNumber.normalizePhoneNumber()
        val ownNumber = ownPhoneNumber.normalizePhoneNumber()
        val callId = CallIdentity.create(
            ownerUserId,
            startedAtMillis,
            callType,
            if (callType == CallType.INCOMING) otherNumber else ownNumber
        )
        return CallRecordMetadata(
            callId = callId,
            ownerUserId = ownerUserId,
            startedAtMillis = startedAtMillis,
            recordingUri = recordingUri,
            phoneNumberFrom = if (callType == CallType.INCOMING) otherNumber else ownNumber,
            phoneNumberTo = if (callType == CallType.INCOMING) ownNumber else otherNumber,
            callType = callType,
            durationSeconds = durationSeconds.coerceAtLeast(0),
            callAtFormatted = callAtFormatted,
            isAnswered = isAnswered,
            careType = careType,
        )
    }

    private fun String?.normalizePhoneNumber(): String? {
        if (this.isNullOrBlank()) return null
        var cleaned = this.trim().replace(Regex("[\\s\\-\\.\\(\\)]"), "")
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3)
        } else if (cleaned.startsWith("84") && cleaned.length >= 11) {
            cleaned = "0" + cleaned.substring(2)
        }
        return cleaned.takeIf { it.isNotEmpty() }
    }
}