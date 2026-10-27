package com.nhakhoaquangninh.telesales.call

import android.content.Context
import android.database.Cursor
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
            queryCallLog(selection, selectionArgs)?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val accountIdIndex = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                val viaNumberIndex = cursor.getColumnIndex(CallLog.Calls.VIA_NUMBER)
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
                    val phoneAccountId = if (accountIdIndex >= 0 && !cursor.isNull(accountIdIndex)) {
                        cursor.getString(accountIdIndex)?.trim()?.takeIf(String::isNotEmpty)
                    } else null
                    val viaNumber = if (viaNumberIndex >= 0 && !cursor.isNull(viaNumberIndex)) {
                        PhoneNumberNormalizer.normalize(cursor.getString(viaNumberIndex))
                    } else null

                    val entry = CallLogEntry(
                        phoneNumber = logNumber,
                        callType = if (androidType == CallLog.Calls.OUTGOING_TYPE) CallType.OUTGOING else CallType.INCOMING,
                        startedAtMillis = startedAt,
                        durationSeconds = cursor.getInt(durationIndex).coerceAtLeast(0),
                        phoneAccountId = phoneAccountId,
                        viaNumber = viaNumber
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

    fun findCallAccount(
        startedAtMillis: Long,
        otherPhoneNumber: String?,
        callType: CallType?
    ): CallLogEntry? {
        if (startedAtMillis <= 0L) return null
        val expectedTypes = when (callType) {
            CallType.OUTGOING -> intArrayOf(CallLog.Calls.OUTGOING_TYPE)
            CallType.INCOMING -> intArrayOf(CallLog.Calls.INCOMING_TYPE, CallLog.Calls.MISSED_TYPE)
            null -> intArrayOf(CallLog.Calls.OUTGOING_TYPE, CallLog.Calls.INCOMING_TYPE, CallLog.Calls.MISSED_TYPE)
        }
        val windowStart = (startedAtMillis - CALL_LOG_TOLERANCE_MILLIS).coerceAtLeast(0L)
        val windowEnd = startedAtMillis + CALL_LOG_TOLERANCE_MILLIS
        val placeholders = expectedTypes.joinToString(",") { "?" }
        val selection = "${CallLog.Calls.TYPE} IN ($placeholders) AND " +
            "${CallLog.Calls.DATE} BETWEEN ? AND ?"
        val selectionArgs = expectedTypes.map(Int::toString).toMutableList().apply {
            add(windowStart.toString())
            add(windowEnd.toString())
        }.toTypedArray()

        return try {
            queryCallLog(selection, selectionArgs)?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val accountIdIndex = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                val viaNumberIndex = cursor.getColumnIndex(CallLog.Calls.VIA_NUMBER)
                val expectedNumber = PhoneNumberNormalizer.normalize(otherPhoneNumber)
                var bestNumberMatch: CallLogEntry? = null
                var bestNumberDiff = Long.MAX_VALUE
                var bestTimeMatch: CallLogEntry? = null
                var bestTimeDiff = Long.MAX_VALUE

                while (cursor.moveToNext()) {
                    val logStartedAt = cursor.getLong(dateIndex)
                    val diff = abs(logStartedAt - startedAtMillis)
                    val androidType = cursor.getInt(typeIndex)
                    val logNumber = PhoneNumberNormalizer.normalize(cursor.getString(numberIndex))
                    val phoneAccountId = if (accountIdIndex >= 0 && !cursor.isNull(accountIdIndex)) {
                        cursor.getString(accountIdIndex)?.trim()?.takeIf(String::isNotEmpty)
                    } else null
                    val viaNumber = if (viaNumberIndex >= 0 && !cursor.isNull(viaNumberIndex)) {
                        PhoneNumberNormalizer.normalize(cursor.getString(viaNumberIndex))
                    } else null

                    val entry = CallLogEntry(
                        phoneNumber = logNumber,
                        callType = if (androidType == CallLog.Calls.OUTGOING_TYPE) CallType.OUTGOING else CallType.INCOMING,
                        startedAtMillis = logStartedAt,
                        durationSeconds = cursor.getInt(durationIndex).coerceAtLeast(0),
                        phoneAccountId = phoneAccountId,
                        viaNumber = viaNumber
                    )

                    if (diff < bestTimeDiff) {
                        bestTimeDiff = diff
                        bestTimeMatch = entry
                    }
                    if (!expectedNumber.isNullOrEmpty() && expectedNumber == logNumber && diff < bestNumberDiff) {
                        bestNumberDiff = diff
                        bestNumberMatch = entry
                    }
                }
                if (!expectedNumber.isNullOrEmpty()) {
                    bestNumberMatch ?: bestTimeMatch?.takeIf { it.phoneNumber.isNullOrEmpty() }
                } else {
                    bestTimeMatch
                }
            }
        } catch (_: Exception) {
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
            queryCallLog(selection, selectionArgs)?.use { cursor ->
                val numberIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val durationIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val dateIndex = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val accountIdIndex = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
                val viaNumberIndex = cursor.getColumnIndex(CallLog.Calls.VIA_NUMBER)
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
                        val phoneAccountId = if (accountIdIndex >= 0 && !cursor.isNull(accountIdIndex)) {
                            cursor.getString(accountIdIndex)?.trim()?.takeIf(String::isNotEmpty)
                        } else null
                        val viaNumber = if (viaNumberIndex >= 0 && !cursor.isNull(viaNumberIndex)) {
                            PhoneNumberNormalizer.normalize(cursor.getString(viaNumberIndex))
                        } else null
                        best = CallLogEntry(
                            phoneNumber = PhoneNumberNormalizer.normalize(cursor.getString(numberIndex)),
                            callType = if (androidType == CallLog.Calls.OUTGOING_TYPE) CallType.OUTGOING else CallType.INCOMING,
                            startedAtMillis = startedAt,
                            durationSeconds = logDuration,
                            phoneAccountId = phoneAccountId,
                            viaNumber = viaNumber
                        )
                    }
                }
                best
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun queryCallLog(selection: String, selectionArgs: Array<String>): Cursor? {
        return try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                EXTENDED_PROJECTION,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )
        } catch (_: IllegalArgumentException) {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                BASE_PROJECTION,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )
        }
    }

    private companion object {
        const val TAG = "CallLogDataSource"
        const val CALL_LOG_TOLERANCE_MILLIS = 60_000L
        val BASE_PROJECTION = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE
        )
        val EXTENDED_PROJECTION = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION,
            CallLog.Calls.DATE,
            CallLog.Calls.PHONE_ACCOUNT_ID,
            CallLog.Calls.VIA_NUMBER
        )
    }
}
