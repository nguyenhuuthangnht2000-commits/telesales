package com.nhakhoaquangninh.telesales.domain.model

enum class CallType(val wireValue: String) {
    INCOMING("incoming"),
    OUTGOING("outgoing");

    companion object {
        fun fromWire(value: String?): CallType? =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
    }
}

enum class FailureReason(val wireValue: String) {
    MISSED("missed"),
    NOT_CONNECTED("not_connected");

    companion object {
        fun fromWire(value: String?): FailureReason? =
            entries.firstOrNull { it.wireValue.equals(value?.trim(), ignoreCase = true) }
    }
}