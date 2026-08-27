package com.nhakhoaquangninh.telesales.domain.model

import java.security.MessageDigest

object CallIdentity {
    fun create(
        ownerUserId: Int,
        startedAtMillis: Long,
        callType: CallType,
        normalizedPhone: String?
    ): String {
        val input = "${ownerUserId}_${startedAtMillis}_${callType.name}_${normalizedPhone ?: ""}"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        val hexString = StringBuilder()
        for (b in hashBytes) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString().take(16)
    }
}
