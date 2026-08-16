package com.nhakhoaquangninh.telesales.ui.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.nhakhoaquangninh.telesales.core.FileLogger

object LogShareUtils {

    /**
     * Chia sẻ tệp log chẩn đoán qua hệ thống chia sẻ của Android (Zalo, Gmail, Telegram, Drive...).
     */
    fun shareLogFile(context: Context) {
        try {
            var file = FileLogger.getLogFile(context)
            if (file == null || !file.exists() || file.length() == 0L) {
                // Tạo một bản ghi đầu tiên nếu file chưa tồn tại
                FileLogger.log(context, "INFO", "Khởi tạo tệp nhật ký chẩn đoán TelesalesApp.")
                file = FileLogger.getLogFile(context)
            }

            if (file == null || !file.exists()) {
                Toast.makeText(context, "Không tìm thấy tệp nhật ký lỗi.", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "TelesalesApp Error Log - ${context.packageName}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Chia sẻ Nhật ký Lỗi qua:").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Không thể chia sẻ tệp log: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
