package com.nhakhoaquangninh.telesales.data.repository

import com.nhakhoaquangninh.telesales.data.remote.dto.ApiErrorBody
import com.google.gson.Gson

/**
 * Parses the server error response body JSON to extract the "message" field.
 * Returns the server message if non-null and non-empty, otherwise returns the fallback.
 */
object ApiErrorParser {

    private val gson = Gson()

    fun getServerMessageOrDefault(errorBodyString: String?, fallbackMessage: String): String {
        if (errorBodyString.isNullOrEmpty()) return fallbackMessage
        return try {
            val parsed = gson.fromJson(errorBodyString, ApiErrorBody::class.java)
            if (!parsed?.message.isNullOrEmpty()) {
                parsed.message
            } else {
                fallbackMessage
            }
        } catch (e: Exception) {
            fallbackMessage
        }
    }
}
