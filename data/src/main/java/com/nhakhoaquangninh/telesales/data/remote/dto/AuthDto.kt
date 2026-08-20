package com.nhakhoaquangninh.telesales.data.remote.dto

import com.google.gson.annotations.SerializedName

data class RequestOtpRequest(
    @SerializedName("user_id")
    val userId: Int
)

data class VerifyOtpRequest(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("otp")
    val otp: String
)

data class LogoutRequest(
    @SerializedName("otp")
    val otp: String
)

data class VerifyOtpData(
    @SerializedName("token")
    val token: String? = null,
    @SerializedName("user")
    val user: UserInfoDto? = null,
    @SerializedName("careTypeOptions", alternate = ["care_type_options"])
    val careTypeOptions: List<CareTypeOptionDto>? = null
)

data class CareTypeOptionDto(
    @SerializedName("value", alternate = ["id", "care_type", "careTypeId", "care_type_id", "type"])
    val value: Int,
    @SerializedName("label", alternate = ["name", "title", "text", "description"])
    val label: String
)

data class UserInfoDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone")
    val phone: String? = null,
    @SerializedName("department")
    val department: String? = null,
    @SerializedName("branch")
    val branch: String? = null,
    @SerializedName("careTypeOptions", alternate = ["care_type_options"])
    val careTypeOptions: List<CareTypeOptionDto>? = null
)

/**
 * Generic API error body parsed from JSON.
 */
data class ApiErrorBody(
    @SerializedName("message")
    val message: String? = null
)
