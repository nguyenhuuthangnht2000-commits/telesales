package com.nhakhoaquangninh.telesales.call

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.nhakhoaquangninh.telesales.core.FileLogger
import com.nhakhoaquangninh.telesales.domain.model.CallRecordingWindow
import com.nhakhoaquangninh.telesales.domain.model.RecordingCandidate
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchPolicy
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchResult

class RecordingLocator(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver

    fun findMatch(call: CallLogEntry): RecordingMatchResult {
        val window = CallRecordingWindow(
            startedAtMillis = call.startedAtMillis,
            endedAtMillis = call.endedAtMillis,
            durationSeconds = call.durationSeconds
        )
        val originalCandidates = queryRecordings(call.endedAtMillis)
        logCandidates(call, window, originalCandidates, false)
        val original = RecordingMatchPolicy.match(window, originalCandidates)
        if (original is RecordingMatchResult.NeedsReview) {
            FileLogger.logLocal(appContext, "RECORDING_SEARCH", "outcome=needs_review reason=${original.reason}")
        }
        if (original != RecordingMatchResult.NotFound) return original
        val fallbackCandidates = queryRecordings(call.endedAtMillis, window)
        logCandidates(call, window, fallbackCandidates, true)
        val fallback = RecordingMatchPolicy.matchFallback(window, call.phoneNumber, fallbackCandidates)
        val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else android.Manifest.permission.READ_EXTERNAL_STORAGE
        val canReadAudio = appContext.checkSelfPermission(readPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        FileLogger.logLocal(appContext, "RECORDING_SEARCH",
            "search_id=${call.startedAtMillis}_${call.callType.wireValue} original_count=${originalCandidates.size} fallback_count=${fallbackCandidates.size} " +
                "read_audio_granted=$canReadAudio " +
                "result=${when (fallback) {
                    is RecordingMatchResult.Matched -> "matched"
                    is RecordingMatchResult.NeedsReview -> "needs_review:${fallback.reason}"
                    RecordingMatchResult.NotFound -> "not_found"
                }} device=${Build.MANUFACTURER}/${Build.MODEL} sdk=${Build.VERSION.SDK_INT}")
        return fallback
    }

    private fun logCandidates(call: CallLogEntry, window: CallRecordingWindow, candidates: List<RecordingCandidate>, fallback: Boolean) {
        val searchId = "${call.startedAtMillis}_${call.callType.wireValue}"
        FileLogger.logLocal(appContext, "RECORDING_CANDIDATES",
            "search_id=$searchId fallback=$fallback count=${candidates.size} logged=${minOf(candidates.size, 20)} " +
                "call_start_ms=${window.startedAtMillis} call_end_ms=${window.endedAtMillis} duration_s=${window.durationSeconds}")
        candidates.take(20).forEach { candidate ->
            FileLogger.logLocal(appContext, "RECORDING_CANDIDATE",
                "search_id=$searchId fallback=$fallback uri=${FileLogger.maskUri(candidate.uri)} " +
                    "source_approved=${RecordingMatchPolicy.isApprovedSource(candidate.relativePath)} " +
                    "mime=${FileLogger.safeDiagnostic(candidate.mimeType)} size_bytes=${candidate.sizeBytes} duration_ms=${candidate.durationMillis} " +
                    "modified_ms=${candidate.modifiedAtMillis} added_ms=${candidate.addedAtMillis} " +
                    "reason=${RecordingMatchPolicy.diagnosticReason(window, call.phoneNumber, candidate, fallback)}")
        }
    }

    fun getApprovedRecordings(): List<RecordingCandidate> =
        queryRecordings(referenceEndMillis = null)
            .asSequence()
            .filter { RecordingMatchPolicy.isApprovedSource(it.relativePath) }
            .filter { it.mimeType?.startsWith("audio/", ignoreCase = true) == true }
            .filter { it.sizeBytes > 0L }
            .distinctBy(RecordingCandidate::uri)
            .sortedByDescending(RecordingCandidate::modifiedAtMillis)
            .toList()

    private fun queryRecordings(
        referenceEndMillis: Long?,
        fallbackWindow: CallRecordingWindow? = null
    ): List<RecordingCandidate> {
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            if (fallbackWindow != null) add(MediaStore.Audio.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()
        val selection = if (fallbackWindow != null) {
            "(${MediaStore.Audio.Media.DATE_MODIFIED} BETWEEN ? AND ?) OR " +
                "(${MediaStore.Audio.Media.DATE_ADDED} BETWEEN ? AND ?)"
        } else referenceEndMillis?.let {
            "${MediaStore.Audio.Media.DATE_MODIFIED} BETWEEN ? AND ?"
        }
        val selectionArgs = if (fallbackWindow != null) {
            val earliest = ((fallbackWindow.startedAtMillis - RecordingMatchPolicy.FALLBACK_EARLY_MILLIS) / 1_000L).toString()
            val latest = ((fallbackWindow.endedAtMillis + RecordingMatchPolicy.FALLBACK_LATE_MILLIS) / 1_000L).toString()
            arrayOf(earliest, latest, earliest, latest)
        } else referenceEndMillis?.let {
            arrayOf(
                ((it - QUERY_EARLY_MILLIS) / 1_000L).toString(),
                ((it + QUERY_LATE_MILLIS) / 1_000L).toString()
            )
        }
        return try {
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                val durationIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val addedIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIndex)
                        add(
                            RecordingCandidate(
                                uri = ContentUris.withAppendedId(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    id
                                ).toString(),
                                displayName = cursor.getString(nameIndex).orEmpty(),
                                mimeType = cursor.getString(mimeIndex),
                                relativePath = pathIndex.takeIf { it >= 0 }?.let(cursor::getString),
                                modifiedAtMillis = cursor.getLong(modifiedIndex) * 1_000L,
                                durationMillis = cursor.getLong(durationIndex).takeIf { it > 0L },
                                sizeBytes = cursor.getLong(sizeIndex),
                                addedAtMillis = addedIndex.takeIf { it >= 0 && !cursor.isNull(it) }
                                    ?.let { cursor.getLong(it).takeIf { seconds -> seconds > 0 }?.times(1_000L) }
                            )
                        )
                    }
                }
            }.orEmpty()
        } catch (_: SecurityException) {
            Log.w(TAG, "Không có quyền đọc MediaStore")
            FileLogger.logLocal(appContext, "RECORDING_SEARCH", "outcome=permission_denied sdk=${Build.VERSION.SDK_INT}")
            emptyList()
        } catch (_: RuntimeException) {
            Log.e(TAG, "Không thể đọc MediaStore")
            FileLogger.logLocal(appContext, "RECORDING_SEARCH", "outcome=query_failed sdk=${Build.VERSION.SDK_INT}")
            emptyList()
        }
    }

    private companion object {
        const val TAG = "RecordingLocator"
        const val QUERY_EARLY_MILLIS = 20_000L
        const val QUERY_LATE_MILLIS = 90_000L
    }
}
