package com.nhakhoaquangninh.telesales

import android.content.Context
import com.nhakhoaquangninh.telesales.domain.common.MessageProvider

class AppMessageProvider(private val context: Context) : MessageProvider {
    override fun getTokenMissingMessage(): String = context.getString(R.string.msg_token_missing)
    override fun getUploadSuccessMessage(): String = context.getString(R.string.msg_upload_success)
    override fun getTokenExpiredMessage(): String = context.getString(R.string.msg_token_expired)
    override fun getUploadFailedMessage(): String = context.getString(R.string.msg_upload_failed)
    override fun getOtpSentMessage(): String = context.getString(R.string.msg_otp_sent)
    override fun getApiKeyInvalidMessage(): String = context.getString(R.string.msg_api_key_invalid)
    override fun getUserNotFoundMessage(userId: Int): String = context.getString(R.string.msg_user_not_found, userId)
    override fun getServerErrorMessage(): String = context.getString(R.string.msg_server_error)
    override fun getOtpRequestFailedMessage(): String = context.getString(R.string.msg_otp_request_failed)
    override fun getServerNoTokenMessage(): String = context.getString(R.string.msg_server_no_token)
    override fun getOtpInvalidMessage(): String = context.getString(R.string.msg_otp_invalid)
    override fun getUserInfoMissingMessage(): String = context.getString(R.string.msg_user_info_missing)
    override fun getOtpVerifyFailedMessage(): String = context.getString(R.string.msg_otp_verify_failed)
}
