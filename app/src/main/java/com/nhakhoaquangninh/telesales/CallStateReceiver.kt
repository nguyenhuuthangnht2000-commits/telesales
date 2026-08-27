package com.nhakhoaquangninh.telesales

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.nhakhoaquangninh.telesales.call.PhoneCallState
import com.nhakhoaquangninh.telesales.call.PhoneNumberNormalizer

class CallStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = when (intent.getStringExtra(TelephonyManager.EXTRA_STATE)) {
            TelephonyManager.EXTRA_STATE_IDLE -> PhoneCallState.IDLE
            TelephonyManager.EXTRA_STATE_OFFHOOK -> PhoneCallState.OFFHOOK
            TelephonyManager.EXTRA_STATE_RINGING -> PhoneCallState.RINGING
            else -> return
        }
        val phoneNumber = PhoneNumberNormalizer.normalize(getIncomingNumberCompat(intent))
        ServiceLocator.init(context.applicationContext)
        val tokenManager = ServiceLocator.tokenManager
        if (tokenManager == null || !tokenManager.isLoggedIn() || !tokenManager.isMonitoringEnabled()) {
            return
        }
        val transition = ServiceLocator.callSessionTracker.onState(state, phoneNumber)
        ServiceLocator.callEventCoordinator.enqueue(transition)
    }

    @Suppress("DEPRECATION")
    private fun getIncomingNumberCompat(intent: Intent): String? =
        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

    companion object {
        const val ACTION_REFRESH_FILES =
            "com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS"
    }
}