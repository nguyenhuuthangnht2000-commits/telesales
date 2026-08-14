package com.nhakhoaquangninh.telesales.core

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiện ích ghi log chẩn đoán sự cố ra tệp văn bản cục bộ trong thư mục Documents của ứng dụng.
 * Tệp log: Android/data/com.nhakhoaquangninh.telesales/files/Documents/telesales_upload_error_log.txt
 */
object FileLogger {
    private const val TAG = "FileLogger"
    private const val FILE_NAME = "telesales_upload_error_log.txt"
    private const val MAX_LOG_SIZE_BYTES = 5L * 1024L * 1024L // Giới hạn 5MB tránh đầy bộ nhớ

    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        try {
            val dir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: return
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, FILE_NAME)
            
            // Nếu file quá 5MB, giữ lại 1 nửa gần nhất để giải phóng
            if (file.exists() && file.length() > MAX_LOG_SIZE_BYTES) {
                val lines = file.readLines()
                if (lines.size > 200) {
                    file.writeText(lines.takeLast(lines.size / 2).joinToString("\n") + "\n")
                }
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = "[$timestamp] [$tag] $message\n----------------------------------------\n"
            file.appendText(entry)
            Log.d(TAG, "Đã ghi log vào file: $entry")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ghi file log: ${e.message}")
        }
    }

    @Synchronized
    fun logException(context: Context, tag: String, message: String, throwable: Throwable) {
        val stackTrace = Log.getStackTraceString(throwable)
        log(context, tag, "$message\nStackTrace:\n$stackTrace")
    }
}
