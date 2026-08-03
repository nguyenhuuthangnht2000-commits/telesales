package com.nhakhoaquangninh.telesales.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Base generic API Response structure wrapping standard fields:
 * - success: Boolean status
 * - message: Human readable status/error message
 * - status: Optional HTTP or business status code
 * - data: Strongly-typed payload T
 */
data class BaseResponse<T>(
    @SerializedName("success")
    val success: Boolean? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("data")
    val data: T? = null
)
