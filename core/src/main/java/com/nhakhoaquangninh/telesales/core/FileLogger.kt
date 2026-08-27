package com.nhakhoaquangninh.telesales.core

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Tiện ích ghi log chẩn đoán sự cố hỗ trợ cơ chế Ghi Log Kép (Dual Logging):
 * 1. Lưu trữ cục bộ trong thư mục Documents của ứng dụng (offline):
 *    Android/data/com.nhakhoaquangninh.telesales/files/Documents/telesales_upload_error_log.txt
 * 2. Đẩy log và Non-Fatal Exceptions lên Firebase Crashlytics để theo dõi từ xa (realtime).
 */
object FileLogger {
    private const val TAG = "FileLogger"
    private const val FILE_NAME = "telesales_upload_error_log.txt"
    private const val MAX_LOG_SIZE_BYTES = 10L * 1024L * 1024L // Giới hạn 10MB (~20.000 cuộc gọi)

    /**
     * Tiện ích che giấu số điện thoại (VD: 0912***678)
     */
    fun maskPhone(phone: String?): String {
        if (phone == null || phone.length < 7) return phone ?: "null"
        val start = phone.substring(0, 4)
        val end = phone.substring(phone.length - 3)
        return "$start***$end"
    }

    /**
     * Tiện ích che giấu URI/Path
     */
    fun maskUri(uri: String?): String {
        if (uri == null) return "null"
        val lastSlash = uri.lastIndexOf("/")
        if (lastSlash != -1 && lastSlash < uri.length - 1) {
            return ".../" + uri.substring(lastSlash + 1)
        }
        return uri
    }

    /**
     * Lấy con trỏ File tới tệp log chẩn đoán cục bộ.
     */
    fun getLogFile(context: Context): File? {
        val dir = context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: return null
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_NAME)
    }

    /**
     * Đọc toàn bộ nội dung tệp log để hiển thị trực tiếp lên giao diện.
     */
    fun readLogContent(context: Context): String {
        return try {
            val file = getLogFile(context) ?: return "Không tìm thấy thư mục lưu tệp log."
            if (!file.exists() || file.length() == 0L) {
                "Chưa có bản ghi lỗi hoặc hoạt động nào."
            } else {
                val text = file.readText()
                if (text.length > 200_000) {
                    "... [Đã ẩn các bản ghi cũ hơn để tối ưu hiển thị, hãy bấm 'Chia sẻ file' để nhận toàn bộ file 10MB] ...\n\n" + text.takeLast(
                        200_000
                    )
                } else {
                    text
                }
            }
        } catch (e: Exception) {
            "Lỗi khi đọc tệp log: ${e.message}"
        }
    }

    /**
     * Xóa sạch nội dung tệp log chẩn đoán.
     */
    fun clearLog(context: Context): Boolean {
        return try {
            val file = getLogFile(context) ?: return false
            if (file.exists()) file.delete() else true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Gắn User ID của nhân viên vào Crashlytics để lọc lỗi theo từng nhân viên trên Firebase Console.
     */
    fun setUserId(userId: String) {
        try {
            FirebaseCrashlytics.getInstance().setUserId(userId)
            Log.d(TAG, "Đã gán Crashlytics User ID: $userId")
        } catch (e: Exception) {
            Log.w(TAG, "Không thể gán Crashlytics User ID: ${e.message}")
        }
    }

    /**
     * Gán thông tin ngữ cảnh (Custom Key) lên Crashlytics (ví dụ: phone_number, duration, http_code).
     */
    fun setCustomKey(key: String, value: String) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể gán Crashlytics Custom Key ($key): ${e.message}")
        }
    }

    fun setCustomKey(key: String, value: Int) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể gán Crashlytics Custom Key ($key): ${e.message}")
        }
    }

    fun setCustomKey(key: String, value: Boolean) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể gán Crashlytics Custom Key ($key): ${e.message}")
        }
    }

    /**
     * Ghi log thông thường vào file cục bộ và Crashlytics timeline.
     */
    @Synchronized
    fun log(context: Context, tag: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$timestamp] [$tag] $message\n----------------------------------------\n"
        // 1. Ghi vào file cục bộ
        try {
            val dir =
                context.applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            if (dir != null) {
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, FILE_NAME)
                // Nếu file quá 5MB, giữ lại 1 nửa gần nhất để giải phóng
                if (file.exists() && file.length() > MAX_LOG_SIZE_BYTES) {
                    val lines = file.readLines()
                    if (lines.size > 200) {
                        file.writeText(lines.takeLast(lines.size / 2).joinToString("\n") + "\n")
                    }
                }

                file.appendText(entry)
            }
            Log.d(TAG, "Đã ghi log vào file: $entry")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi ghi file log: ${e.message}")
        }

        // 2. Gửi log lên Firebase Crashlytics timeline
        try {
            FirebaseCrashlytics.getInstance().log("[$tag] $message")
        } catch (e: Exception) {
            Log.w(TAG, "Không thể gửi log lên Crashlytics: ${e.message}")
        }
    }

    /**
     * Ghi nhận Exception vào file cục bộ và bắn Non-Fatal Exception lên Firebase Crashlytics.
     */
    @Synchronized
    fun logException(context: Context, tag: String, message: String, throwable: Throwable) {
        val stackTrace = Log.getStackTraceString(throwable)
        log(context, tag, "$message\nStackTrace:\n$stackTrace")

        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("last_error_tag", tag)
            crashlytics.setCustomKey("last_error_msg", message)
            crashlytics.recordException(throwable)
        } catch (e: Exception) {
            Log.w(TAG, "Không thể gửi exception lên Crashlytics: ${e.message}")
        }
    }

    /**
     * Ghi nhận lỗi phi ngoại lệ (Non-Fatal issue như HTTP 401, 500, lỗi Metadata, lỗi File) lên Crashlytics.
     */
    @Synchronized
    fun logNonFatalError(
        context: Context,
        tag: String,
        message: String,
        customKeys: Map<String, Any?> = emptyMap()
    ) {
        log(context, tag, message)

        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("error_tag", tag)
            crashlytics.setCustomKey("error_message", message)
            customKeys.forEach { (k, v) ->
                when (v) {
                    is String -> crashlytics.setCustomKey(k, v)
                    is Int -> crashlytics.setCustomKey(k, v)
                    is Long -> crashlytics.setCustomKey(k, v)
                    is Float -> crashlytics.setCustomKey(k, v)
                    is Double -> crashlytics.setCustomKey(k, v)
                    is Boolean -> crashlytics.setCustomKey(k, v)
                    else -> if (v != null) crashlytics.setCustomKey(k, v.toString())
                }
            }
            crashlytics.recordException(TelesalesNonFatalException("[$tag] $message"))
        } catch (e: Exception) {
            Log.w(TAG, "Không thể ghi nhận Non-Fatal error lên Crashlytics: ${e.message}")
        }
    }
}

/**
 * Lớp Exception tùy biến đại diện cho các lỗi Non-Fatal (Lỗi mạng, Server từ chối, File rỗng...).
 */
class TelesalesNonFatalException(message: String) : Exception(message)
