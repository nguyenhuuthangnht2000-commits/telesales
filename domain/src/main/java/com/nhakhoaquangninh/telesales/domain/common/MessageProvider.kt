package com.nhakhoaquangninh.telesales.domain.common

interface MessageProvider {
    fun getTokenMissingMessage(): String
    fun getUploadSuccessMessage(): String
    fun getTokenExpiredMessage(): String
    fun getUploadFailedMessage(): String
    fun getOtpSentMessage(): String
    fun getApiKeyInvalidMessage(): String
    fun getUserNotFoundMessage(userId: Int): String
    fun getServerErrorMessage(): String
    fun getOtpRequestFailedMessage(): String
    fun getServerNoTokenMessage(): String
    fun getOtpInvalidMessage(): String
    fun getUserInfoMissingMessage(): String
    fun getOtpVerifyFailedMessage(): String
}
