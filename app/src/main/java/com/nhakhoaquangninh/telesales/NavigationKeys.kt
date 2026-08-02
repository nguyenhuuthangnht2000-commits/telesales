package com.nhakhoaquangninh.telesales

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Login : NavKey

@Serializable
data class OtpVerify(val userId: Int) : NavKey

@Serializable
data object Main : NavKey

@Serializable
data object VoIPCall : NavKey
