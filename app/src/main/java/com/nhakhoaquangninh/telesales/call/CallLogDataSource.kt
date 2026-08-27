package com.nhakhoaquangninh.telesales.call

import android.content.Context
import android.provider.CallLog
import android.util.Log
import com.nhakhoaquangninh.telesales.domain.model.CallType
import kotlin.math.abs

class CallLogDataSource(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun findClosest(snapshot: CallSessionSnapshot, missedIncoming: Boolean): CallLogEntry? {
        val expectedTypes = when {
            missedIncoming -> intArrayOf(CallLog.Calls.MISSED_TYPE, CallLog.Calls.INCOMING_TYPE)
            snapshot.incoming -> intArrayOf(CallLog.Calls.INCOMING_TYPE)
            else -> intArrayOf(CallLog.Calls.OUTGOING_TYPE)
        }
        val reference = snapshot.startedAtMillis.takeIf { it > 0L } ?: return null
        val windowStart = (reference - CALL_LOG_TOLERANCE_MILLIS).coerceAtLeast(0L)
        val windowEnd = snapshot.endedAtMillis + CALL_LOG_TOLERANCE_MILLIS
        val placeholders = expectedTypes.joinToString(",") { "?" }
        val selection = "${CallLog.Calls.TYPE} IN ($placeholders) AND " +
            "${CallLog.Calls.DATE} BETWEEN ? AND ?"
        val selectionArgs = expectedTypes.map(Int::toString).toMutableList().apply {
            add(windowStart.toString())
            add(windowEnd.toString())
        }.toTypedArray()

        return try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE
                ),
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                var bestNumberMatch: CallLogEntry? = null
                var bestNumberMatchDifference = Long.MAX_VALUE
                var bestTimeMatch: CallLogEntry? = null
                var bestTimeMatchDifference = Long.MAX_VALUE
                val expectedNumber = PhoneNumberNormalizer.normalize(snapshot.otherPhoneNumber)

                while (cursor.moveToNext()) {
                    val startedAt = cursor.getLong(dateIndex)
                    val difference = abs(startedAt - reference)
                    val androidType = cursor.getInt(typeIndex)
                    val logNumber = PhoneNumberNormalizer.normalize(cursor.getString(numberIndex))
                    
                    val entry = CallLogEntry(
                        phoneNumber = logNumber,
                        callType = if (androidType == CallLog.Calls.OUTGOING_TYPE) CallType.OUTGOING else CallType.INCOMING,
                        startedAtMillis = startedAt,
                        durationSeconds = cursor.getInt(durationIndex).coerceAtLeast(0)
                    )
                    
                    if (difference < bestTimeMatchDifference) {
                        bestTimeMatchDifference = difference
                        bestTimeMatch = entry
                    }
                    
                    if (!expectedNumber.isNullOrEmpty() && expectedNumber == logNumber) {
                        if (difference < bestNumberMatchDifference) {
                            bestNumberMatchDifference = difference
                            bestNumberMatch = entry
                        }
                    }
                }
                if (!expectedNumber.isNullOrEmpty()) {
                    bestNumberMatch
                } else {
                    bestTimeMatch
                }
            }
        } catch (_: SecurityException) {
            Log.w(TAG, "Không có quyền đọc CallLog")
            null
        } catch (_: RuntimeException) {
            Log.e(TAG, "Không thể đọc CallLog")
            null
        }
    }

    fun recoverFromTime(endedAtMillis: Long, durationSeconds: Int): CallLogEntry? {
        if (durationSeconds <= 0) return null
        val durationMillis = durationSeconds * 1_000L
        val estimatedStartedAt = (endedAtMillis - durationMillis).coerceAtLeast(0L)
        val windowStart = (estimatedStartedAt - CALL_LOG_TOLERANCE_MILLIS).coerceAtLeast(0L)
        val windowEnd = endedAtMillis + CALL_LOG_TOLERANCE_MILLIS
        val expectedTypes = intArrayOf(CallLog.Calls.INCOMING_TYPE, CallLog.Calls.OUTGOING_TYPE)
        val placeholders = expectedTypes.joinToString(",") { "?" }
        val selection = "${CallLog.Calls.TYPE} IN ($placeholders) AND " +
            "${CallLog.Calls.DATE} BETWEEN ? AND ?"
        val selectionArgs = expectedTypes.map(Int::toString).toMutableList().apply {
            add(windowStart.toString())
            add(windowEnd.toString())
        }.toTypedArray()
        return try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DURATION,
                    CallLog.Calls.DATE
                ),
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                var best: CallLogEntry? = null
                var bestDifference = Long.MAX_VALUE
                while (cursor.moveToNext()) {
                    val logDuration = cursor.getInt(durationIndex).coerceAtLeast(0)
                    if (logDuration <= 0) continue
                    val durationDiff = abs(logDuration - durationSeconds)
                    val tolerance = maxOf(10.0, durationSeconds * 0.25)
                    if (durationDiff > tolerance) continue
                    val startedAt = cursor.getLong(dateIndex)
                    val difference = abs(startedAt - estimatedStartedAt)
                    if (difference < bestDifference) {
                        bestDifference = difference
                        val androidType = cursor.getInt(typeIndex)
                        best = CallLogEntry(
                            phoneNumber = PhoneNumberNormalizer.normalize(cursor.getString(numberIndex)),
                            callType = if (androidType == CallLog.Calls.OUTGOING_TYPE) CallType.OUTGOING else CallType.INCOMING,
                            startedAtMillis = startedAt,
                            durationSeconds = logDuration
                        )
                    }
                }
                best
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val TAG = "CallLogDataSource"
        const val CALL_LOG_TOLERANCE_MILLIS = 60_000L
    }
}
