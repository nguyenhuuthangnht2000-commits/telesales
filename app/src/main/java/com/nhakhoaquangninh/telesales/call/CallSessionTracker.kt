package com.nhakhoaquangninh.telesales.call

enum class PhoneCallState { IDLE, RINGING, OFFHOOK }

data class CallSessionSnapshot(
    val sessionId: Long,
    val incoming: Boolean,
    val otherPhoneNumber: String?,
    val startedAtMillis: Long,
    val endedAtMillis: Long
)

sealed interface CallTransition {
    data object None : CallTransition
    data class ConnectedEnded(val snapshot: CallSessionSnapshot) : CallTransition
    data class MissedIncomingEnded(val snapshot: CallSessionSnapshot) : CallTransition
}

class CallSessionTracker(
    private val clock: () -> Long = System::currentTimeMillis
) {
    private var lastState = PhoneCallState.IDLE
    private var sessionId = 0L
    private var incoming = false
    private var number: String? = null
    private var startedAtMillis = 0L
    private var answered = false

    @Synchronized
    fun onState(state: PhoneCallState, incomingNumber: String?): CallTransition {
        val normalizedNumber = incomingNumber?.trim()?.takeIf(String::isNotEmpty)
        if (state == lastState) {
            normalizedNumber?.let { number = it }
            return CallTransition.None
        }

        val transition = when (state) {
            PhoneCallState.RINGING -> {
                sessionId++
                incoming = true
                normalizedNumber?.let { number = it }
                startedAtMillis = clock()
                answered = false
                CallTransition.None
            }

            PhoneCallState.OFFHOOK -> {
                if (lastState != PhoneCallState.RINGING) {
                    sessionId++
                    incoming = false
                    number = normalizedNumber
                    startedAtMillis = clock()
                }
                answered = true
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
                clear()
                result
            }
        }
        lastState = state
        return transition
    }

    private fun snapshot(endedAtMillis: Long) = CallSessionSnapshot(
        sessionId = sessionId,
        incoming = incoming,
        otherPhoneNumber = number,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis
    )

    private fun clear() {
        incoming = false
        number = null
        startedAtMillis = 0L
        answered = false
    }
}
