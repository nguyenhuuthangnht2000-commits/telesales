package com.nhakhoaquangninh.telesales.call

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.nhakhoaquangninh.telesales.domain.model.CallRecordingWindow
import com.nhakhoaquangninh.telesales.domain.model.RecordingCandidate
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchPolicy
import com.nhakhoaquangninh.telesales.domain.model.RecordingMatchResult

class RecordingLocator(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun findMatch(call: CallLogEntry): RecordingMatchResult {
        val window = CallRecordingWindow(
            startedAtMillis = call.startedAtMillis,
            endedAtMillis = call.endedAtMillis,
            durationSeconds = call.durationSeconds
        )
        return RecordingMatchPolicy.match(window, queryRecordings(call.endedAtMillis))
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

    private fun queryRecordings(referenceEndMillis: Long?): List<RecordingCandidate> {
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            add(MediaStore.Audio.Media.MIME_TYPE)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()
        val selection = referenceEndMillis?.let {
            "${MediaStore.Audio.Media.DATE_MODIFIED} BETWEEN ? AND ?"
        }
        val selectionArgs = referenceEndMillis?.let {
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
                                sizeBytes = cursor.getLong(sizeIndex)
                            )
                        )
                    }
                }
            }.orEmpty()
        } catch (_: SecurityException) {
            Log.w(TAG, "Không có quyền đọc MediaStore")
            emptyList()
        } catch (_: RuntimeException) {
            Log.e(TAG, "Không thể đọc MediaStore")
            emptyList()
        }
    }

    private companion object {
        const val TAG = "RecordingLocator"
        const val QUERY_EARLY_MILLIS = 20_000L
        const val QUERY_LATE_MILLIS = 90_000L
    }
}
