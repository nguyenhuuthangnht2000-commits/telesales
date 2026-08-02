package com.nhakhoaquangninh.telesales.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RequestOtpRequest(
    @SerializedName("user_id")
    val userId: Int
)

data class RequestOtpResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("status")
    val status: Int? = null
)

data class VerifyOtpRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("otp")
    val otp: String
)

data class VerifyOtpResponse(
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("user")
    val user: UserInfoDto? = null
)

data class UserInfoDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null
)

/**
 * Generic API error body parsed from JSON.
 * Server is expected to return { "message": "...", ... } for error responses.
 */
data class ApiErrorBody(
    @SerializedName("message")
    val message: String? = null
)
