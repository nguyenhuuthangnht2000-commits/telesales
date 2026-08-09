package com.nhakhoaquangninh.telesales

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.nhakhoaquangninh.telesales.data.local.TokenManager

object OwnPhoneNumberResolver {
    private const val TAG = "OwnPhoneNumberResolver"

    fun resolve(context: Context): String? {
        val appContext = context.applicationContext
        TokenManager.getInstance(appContext).getSession()?.phoneNumber.normalize()
            ?.let { return it }

        if (ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) return null

        return try {
            val manager =
                appContext.getSystemService(SubscriptionManager::class.java) ?: return null
            manager.activeSubscriptionInfoList.orEmpty().firstNotNullOfOrNull { subscription ->
                val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    manager.getPhoneNumber(subscription.subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    subscription.number
                }
                number.normalize()
            }
        } catch (_: SecurityException) {
            Log.w(TAG, "Không thể đọc số điện thoại của SIM")
            null
        } catch (_: RuntimeException) {
            Log.w(TAG, "Thiết bị không cung cấp số điện thoại của SIM")
            null
        }
    }

    private fun String?.normalize(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}