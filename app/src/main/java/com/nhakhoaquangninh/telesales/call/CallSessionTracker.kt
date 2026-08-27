package com.nhakhoaquangninh.telesales.call

import android.content.SharedPreferences
import androidx.core.content.edit

enum class PhoneCallState { IDLE, RINGING, OFFHOOK }

data class CallSessionSnapshot(
    val sessionId: Long,
    val incoming: Boolean,
    val otherPhoneNumber: String?,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val ownerUserId: Int,
    val ownPhoneNumber: String?,
    val careType: Int?,
    val answered: Boolean
)

sealed interface CallTransition {
    data object None : CallTransition
    data class ConnectedEnded(val snapshot: CallSessionSnapshot) : CallTransition
    data class MissedIncomingEnded(val snapshot: CallSessionSnapshot) : CallTransition
}

class CallSessionTracker(
    private val prefs: SharedPreferences,
    private val getOwnerUserId: () -> Int?,
    private val getOwnPhoneNumber: () -> String?,
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var lastState = PhoneCallState.IDLE
    private var sessionId = 0L
    private var incoming = false
    private var number: String? = null
    private var startedAtMillis = 0L
    private var answered = false
    private var ownerUserId = -1
    private var ownPhoneNumber: String? = null

    init {
        restoreState()
    }

    @Synchronized
    fun onState(state: PhoneCallState, incomingNumber: String?): CallTransition {
        val normalizedNumber = incomingNumber?.trim()?.takeIf(String::isNotEmpty)
        
        val currentUserId = getOwnerUserId()
        if (currentUserId == null) {
            clearState()
            return CallTransition.None
        }

        if (state == lastState) {
            normalizedNumber?.let { 
                number = it
                saveState()
            }
            return CallTransition.None
        }

        val transition = when (state) {
            PhoneCallState.RINGING -> {
                sessionId++
                incoming = true
                normalizedNumber?.let { number = it }
                startedAtMillis = clock()
                answered = false
                ownerUserId = currentUserId
                ownPhoneNumber = getOwnPhoneNumber()
                saveState()
                CallTransition.None
            }

            PhoneCallState.OFFHOOK -> {
                if (lastState != PhoneCallState.RINGING) {
                    sessionId++
                    incoming = false
                    number = normalizedNumber
                    startedAtMillis = clock()
                    ownerUserId = currentUserId
                    ownPhoneNumber = getOwnPhoneNumber()
                }
                answered = true
                saveState()
                CallTransition.None
            }

            PhoneCallState.IDLE -> {
                val snapshot = snapshot(clock())
                val result = when {
                    lastState == PhoneCallState.OFFHOOK && answered ->
                        CallTransition.ConnectedEnded(snapshot)

                    incoming && lastState == PhoneCallState.RINGING ->
                        CallTransition.MissedIncomingEnded(snapshot)

                    else -> CallTransition.None
                }
                clearState()
                result
            }
        }
        lastState = state
        saveState()
        return transition
    }

    private fun snapshot(endedAtMillis: Long) = CallSessionSnapshot(
        sessionId = sessionId,
        incoming = incoming,
        otherPhoneNumber = number,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis,
        ownerUserId = ownerUserId,
        ownPhoneNumber = ownPhoneNumber,
        careType = null,
        answered = answered
    )

    private fun saveState() {
        prefs.edit {
            putString("lastState", lastState.name)
                .putLong("sessionId", sessionId)
                .putBoolean("incoming", incoming)
                .putString("number", number)
                .putLong("startedAtMillis", startedAtMillis)
                .putBoolean("answered", answered)
                .putInt("ownerUserId", ownerUserId)
                .putString("ownPhoneNumber", ownPhoneNumber)
        }
    }

    private fun restoreState() {
        try {
            val stateName = prefs.getString("lastState", PhoneCallState.IDLE.name) ?: PhoneCallState.IDLE.name
            lastState = PhoneCallState.valueOf(stateName)
            sessionId = prefs.getLong("sessionId", 0L)
            incoming = prefs.getBoolean("incoming", false)
            number = prefs.getString("number", null)
            startedAtMillis = prefs.getLong("startedAtMillis", 0L)
            answered = prefs.getBoolean("answered", false)
            ownerUserId = prefs.getInt("ownerUserId", -1)
            ownPhoneNumber = prefs.getString("ownPhoneNumber", null)
        } catch (e: Exception) {
            clearState()
        }
    }

    private fun clearState() {
        lastState = PhoneCallState.IDLE
        incoming = false
        number = null
        startedAtMillis = 0L
        answered = false
        ownerUserId = -1
        ownPhoneNumber = null
        prefs.edit { clear() }
    }
}
