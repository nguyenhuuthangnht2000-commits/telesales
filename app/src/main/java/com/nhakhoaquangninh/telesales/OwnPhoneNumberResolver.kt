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

    /** Diagnostic only: does not change which number is used by resolve(). */
    fun logDiagnostics(context: Context, callId: String, workId: String) {
        val appContext = context.applicationContext
        val account = TokenManager.getInstance(appContext).getSession()?.phoneNumber.normalize()
        val granted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
        var state = if (granted) "available" else "permission_denied"
        val numbers = mutableListOf<String>()
        val sims = if (!granted) "unknown" else try {
            val manager = appContext.getSystemService(SubscriptionManager::class.java)
            manager?.activeSubscriptionInfoList.orEmpty().joinToString(";") { sub ->
                val number = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    manager?.getPhoneNumber(sub.subscriptionId)
                } else {
                    @Suppress("DEPRECATION")
                    sub.number
                }
                number.normalize()?.let(numbers::add)
                "slot=${sub.simSlotIndex},subscription=${sub.subscriptionId},phone=${com.nhakhoaquangninh.telesales.core.FileLogger.maskPhone(number)}"
            }.ifEmpty { state = "no_active_subscription"; "none" }
        } catch (_: SecurityException) {
            state = "permission_denied"; "unknown"
        } catch (_: RuntimeException) {
            state = "query_failed"; "unknown"
        }
        fun canonical(value: String) = value.filter(Char::isDigit).let {
            if (it.startsWith("84") && it.length == 11) "0" + it.drop(2) else it
        }
        val mismatch = if (state != "available" || account == null || numbers.isEmpty()) "unknown"
            else numbers.none { canonical(it) == canonical(account) }.toString()
        com.nhakhoaquangninh.telesales.core.FileLogger.logLocal(appContext, "PHONE_SOURCE",
            "call_id=$callId work_id=$workId priority=sim_then_account observed_source=${if (state == "available" && numbers.isNotEmpty()) "sim" else if (state == "query_failed") "unknown" else if (account != null) "account_fallback" else "unavailable"} " +
                "account_phone=${com.nhakhoaquangninh.telesales.core.FileLogger.maskPhone(account)} " +
                "sim_state=$state sims=[$sims] account_matches_no_sim=$mismatch call_subscription=unknown")
    }

    fun resolve(context: Context): String? {
        val appContext = context.applicationContext
        return selectPhoneNumber(
            simNumber = { readSimNumber(appContext) },
            userNumber = { TokenManager.getInstance(appContext).getSession()?.phoneNumber }
        )
    }

    internal fun selectPhoneNumber(simNumber: () -> String?, userNumber: () -> String?): String? =
        simNumber().normalize() ?: userNumber().normalize()

    private fun readSimNumber(appContext: Context): String? {
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
