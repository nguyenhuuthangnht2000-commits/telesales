package com.nhakhoaquangninh.telesales

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.nhakhoaquangninh.telesales.call.PhoneCallState
import com.nhakhoaquangninh.telesales.call.PhoneNumberNormalizer
import com.nhakhoaquangninh.telesales.call.CallTransition

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
        if (transition == CallTransition.None) return
        val sessionId = when (transition) {
            is CallTransition.ConnectedEnded -> transition.snapshot.sessionId
            is CallTransition.MissedIncomingEnded -> transition.snapshot.sessionId
            CallTransition.None -> return
        }
        val pendingResult = goAsync()
        val handler = Handler(Looper.getMainLooper())
        var finished = false
        lateinit var deadline: Runnable
        fun finish() {
            if (finished) return
            finished = true
            handler.removeCallbacks(deadline)
            pendingResult.finish()
        }
        // Leave headroom under the receiver timeout even if WorkManager is slow to persist.
        deadline = Runnable { finish() }
        handler.postDelayed(deadline, RECEIVER_TIMEOUT_MILLIS)
        fun enqueue(endedTransition: CallTransition) {
            try {
                val operation = ServiceLocator.callEventCoordinator.enqueue(endedTransition)
                if (operation == null) {
                    finish()
                } else {
                    operation.result.addListener({
                        try {
                            // The listener runs only after completion; this does not block main.
                            operation.result.get()
                        } catch (_: Exception) {
                            Log.e(TAG, "Không thể lưu tác vụ xử lý cuộc gọi")
                        } finally {
                            finish()
                        }
                    }, ContextCompat.getMainExecutor(context.applicationContext))
                }
            } catch (_: RuntimeException) {
                Log.e(TAG, "Không thể đưa cuộc gọi vào hàng đợi")
                finish()
            }
        }
        try {
            ServiceLocator.callLocationProvider.capture(sessionId) { location ->
                val enriched = try {
                    val ownerUserId = when (transition) {
                        is CallTransition.ConnectedEnded -> transition.snapshot.ownerUserId
                        is CallTransition.MissedIncomingEnded -> transition.snapshot.ownerUserId
                        CallTransition.None -> null
                    }
                    val currentSession = ServiceLocator.tokenManager
                    val acceptedLocation = location.takeIf {
                        currentSession != null && currentSession.isLoggedIn() &&
                            currentSession.isMonitoringEnabled() && currentSession.getUserId() == ownerUserId
                    }
                    if (location != null && acceptedLocation == null) {
                        com.nhakhoaquangninh.telesales.core.FileLogger.logLocal(context.applicationContext, "API_LOG", "location_capture session_id=$sessionId outcome=session_changed location_available=false")
                    }
                    when (transition) {
                        is CallTransition.ConnectedEnded -> transition.copy(
                            snapshot = transition.snapshot.copy(latitude = acceptedLocation?.latitude, longitude = acceptedLocation?.longitude)
                        )
                        is CallTransition.MissedIncomingEnded -> transition.copy(
                            snapshot = transition.snapshot.copy(latitude = acceptedLocation?.latitude, longitude = acceptedLocation?.longitude)
                        )
                        CallTransition.None -> transition
                    }
                } catch (_: RuntimeException) {
                    com.nhakhoaquangninh.telesales.core.FileLogger.logLocal(context.applicationContext, "API_LOG", "location_capture session_id=$sessionId outcome=session_check_failed location_available=false")
                    transition
                }
                enqueue(enriched)
            }
        } catch (_: RuntimeException) {
            // Optional location setup must never discard the original ended-call event.
            com.nhakhoaquangninh.telesales.core.FileLogger.logLocal(context.applicationContext, "API_LOG", "location_capture session_id=$sessionId outcome=provider_error location_available=false")
            enqueue(transition)
        }
    }

    @Suppress("DEPRECATION")
    private fun getIncomingNumberCompat(intent: Intent): String? =
        intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

    companion object {
        private const val TAG = "CallStateReceiver"
        private const val RECEIVER_TIMEOUT_MILLIS = 8_000L
        const val ACTION_REFRESH_FILES =
            "com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS"
    }
}
