package com.nhakhoaquangninh.telesales.call

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

sealed interface RecordingUriValidation {
    data class Valid(
        val displayName: String,
        val mimeType: String,
        val sizeBytes: Long
    ) : RecordingUriValidation

    data class Invalid(val reason: String) : RecordingUriValidation
}

object RecordingUriValidator {
    private const val MAX_SIZE_BYTES = 50L * 1024L * 1024L

    fun validate(context: Context, uriValue: String?): RecordingUriValidation {
        if (uriValue.isNullOrBlank()) return RecordingUriValidation.Invalid("missing_uri")
        val uri = runCatching { Uri.parse(uriValue) }.getOrNull()
            ?: return RecordingUriValidation.Invalid("invalid_uri")
        if (uri.scheme != "content") return RecordingUriValidation.Invalid("unsupported_uri_scheme")
        val resolver = context.applicationContext.contentResolver
        return try {
            val mimeType = resolver.getType(uri)
                ?.takeIf { it.startsWith("audio/", ignoreCase = true) }
                ?: return RecordingUriValidation.Invalid("invalid_mime_type")
            var displayName = "recording"
            var sizeBytes = -1L
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex >= 0) displayName = cursor.getString(nameIndex).orEmpty()
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
                }
            }
            if (sizeBytes <= 0L) return RecordingUriValidation.Invalid("empty_recording")
            if (sizeBytes > MAX_SIZE_BYTES) return RecordingUriValidation.Invalid("recording_too_large")
            resolver.openAssetFileDescriptor(uri, "r")?.use { } 
                ?: return RecordingUriValidation.Invalid("recording_unreadable")
            RecordingUriValidation.Valid(displayName, mimeType, sizeBytes)
        } catch (_: SecurityException) {
            RecordingUriValidation.Invalid("recording_permission_denied")
        } catch (_: RuntimeException) {
            RecordingUriValidation.Invalid("recording_unreadable")
        }
    }
}
