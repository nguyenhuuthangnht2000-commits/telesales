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
        if (number.isNullOrBlank()) return null
        val trimmed = number.trim()
        if (trimmed.uppercase(Locale.ROOT) in hiddenValues) return null

        // Loại bỏ khoảng trắng, dấu gạch nối, dấu chấm, dấu ngoặc đơn
        var cleaned = trimmed.replace(Regex("[\\s\\-\\.\\(\\)]"), "")
        
        // Chuẩn hóa tiền tố quốc tế Việt Nam (+84 hoặc 84) về đầu số 0 chuẩn
        if (cleaned.startsWith("+84")) {
            cleaned = "0" + cleaned.substring(3)
        } else if (cleaned.startsWith("84") && cleaned.length >= 11) {
            cleaned = "0" + cleaned.substring(2)
        }
        
        return cleaned.takeIf { it.isNotEmpty() }
    }

    /**
     * Kiểm tra số điện thoại có đúng chuẩn định dạng (10-11 chữ số tại Việt Nam) hay không.
     */
    fun isValid(number: String?): Boolean {
        val normalized = normalize(number) ?: return false
        return normalized.length in 10..11 && normalized.all { it.isDigit() }
    }

    /**
     * Kiểm tra xem số điện thoại có bị thiếu số do bấm nhầm hay không (dưới 9 chữ số).
     */
    fun isLikelyIncomplete(number: String?): Boolean {
        val normalized = normalize(number) ?: return false
        return normalized.length < 9 && normalized.all { it.isDigit() }
    }
}
