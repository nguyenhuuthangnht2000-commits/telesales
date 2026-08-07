package com.nhakhoaquangninh.telesales.call

import java.util.Locale

object PhoneNumberNormalizer {
    private val hiddenValues = setOf(
        "UNKNOWN",
        "PRIVATE",
        "PRIVATE NUMBER",
        "BLOCKED",
        "ANONYMOUS",
        "-1"
    )

    fun normalize(number: String?): String? {
        val normalized = number?.trim()?.takeIf(String::isNotEmpty) ?: return null
        return normalized.takeUnless { it.uppercase(Locale.ROOT) in hiddenValues }
    }
}
