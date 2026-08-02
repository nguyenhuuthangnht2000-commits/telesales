package com.nhakhoaquangninh.telesales

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import com.nhakhoaquangninh.telesales.CallStateReceiver.Companion.FILE_FRESHNESS_MS
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CallStateReceiver v2 — KHÔNG TỰ GHI ÂM.
 *
 * Chiến lược:
 *  1. Lắng nghe IDLE sau OFFHOOK → cuộc gọi vừa kết thúc.
 *  2. Đợi 3 giây (chờ ứng dụng ghi âm tích hợp của máy lưu file xong).
 *  3. Quét các thư mục phổ biến để tìm file .mp3/.amr mới tạo ≤ 10 giây trước.
 *  4. Tìm thấy → in đường dẫn + enqueue upload.
 *  5. KHÔNG tìm thấy → hiển thị WarningActivity toàn màn hình.
 */
class CallStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallStateReceiver"
        const val ACTION_REFRESH_FILES = "com.nhakhoaquangninh.telesales.REFRESH_RECORDINGS"

        // Delay chờ ứng dụng ghi âm tích hợp lưu file xong
        private const val SCAN_DELAY_MS = 3_000L

        // Ngưỡng "file mới": tạo trong vòng N giây trước thời điểm quét
        private const val FILE_FRESHNESS_MS = 10_000L

        // Theo dõi transition trạng thái (dùng companion object để duy trì qua các lần nhận broadcast)
        private var lastState = TelephonyManager.CALL_STATE_IDLE
        private var savedNumber: String? = null
        private var callAnswered = false     // true khi đã sang OFFHOOK (bắt máy)

        /**
         * Trả về danh sách thư mục ghi âm có ưu tiên theo hãng máy thực tế.
         *
         * Cách hoạt động:
         *  1. Đọc Build.MANUFACTURER và Build.MODEL.
         *  2. Xếp thư mục của hãng đó lên đầu danh sách.
         *  3. Thêm toàn bộ thư mục còn lại vào sau (fallback).
         *  4. Chỉ giữ những thư mục thực sự tồn tại trên máy.
         */
        fun getDirectoriesForDevice(): List<String> {
            val manufacturer = Build.MANUFACTURER.lowercase(Locale.ROOT)
            val model = Build.MODEL.lowercase(Locale.ROOT)

            Log.d(
                TAG,
                "📱 Thiết bị: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.SDK_INT})"
            )

            // ── Thư mục ưu tiên theo hãng máy ──────────────────────────────
            val priorityDirs: List<String> = when {

                // Samsung
                manufacturer.contains("samsung") -> listOf(
                    "/storage/emulated/0/Call/",
                    "/storage/emulated/0/Record/Call/",
                    "/storage/emulated/0/Recordings/Call/",
                    "/storage/emulated/0/DCIM/Call/",
                    "/storage/emulated/0/PhoneRecord/"
                )

                // Xiaomi / Redmi / POCO / MIUI
                manufacturer.contains("xiaomi") ||
                        manufacturer.contains("redmi") ||
                        model.contains("redmi") ||
                        model.contains("poco") -> listOf(
                    "/storage/emulated/0/MIUI/sound_recorder/call_rec/",
                    "/storage/emulated/0/Recordings/Call Records/",
                    "/storage/emulated/0/Record/Call/"
                )

                // OPPO
                manufacturer.contains("oppo") -> listOf(
                    "/storage/emulated/0/Recordings/Call/",
                    "/storage/emulated/0/ColorOS/Recorder/Call/",
                    "/storage/emulated/0/Record/Call/"
                )

                // Realme (OPPO sub-brand, cùng đường dẫn)
                manufacturer.contains("realme") -> listOf(
                    "/storage/emulated/0/Recordings/Call/",
                    "/storage/emulated/0/ColorOS/Recorder/Call/",
                    "/storage/emulated/0/Record/Call/"
                )

                // OnePlus (OxygenOS — cũng gốc OPPO)
                manufacturer.contains("oneplus") -> listOf(
                    "/storage/emulated/0/Recordings/",
                    "/storage/emulated/0/Record/"
                )

                // Vivo
                manufacturer.contains("vivo") -> listOf(
                    "/storage/emulated/0/Vivo/callrecord/",
                    "/storage/emulated/0/record/callrec/",
                    "/storage/emulated/0/Recordings/Call/"
                )

                // Huawei / Honor
                manufacturer.contains("huawei") ||
                        manufacturer.contains("honor") -> listOf(
                    "/storage/emulated/0/Sounds/",
                    "/storage/emulated/0/Record/",
                    "/storage/emulated/0/PhoneRecord/"
                )

                // Motorola
                manufacturer.contains("motorola") -> listOf(
                    "/storage/emulated/0/Recordings/",
                    "/storage/emulated/0/AudioRecorder/"
                )

                // Nokia / HMD
                manufacturer.contains("nokia") ||
                        manufacturer.contains("hmd") -> listOf(
                    "/storage/emulated/0/AudioRecorder/",
                    "/storage/emulated/0/Recordings/"
                )

                // Android One / Android thuần (Google, Nokia...)
                else -> listOf(
                    "/storage/emulated/0/Recordings/",
                    "/storage/emulated/0/AudioRecorder/",
                    "/storage/emulated/0/Record/"
                )
            }

            // ── Fallback: toàn bộ thư mục biết được (sau priority) ─────────────
            val fallbackDirs = listOf(
                "/storage/emulated/0/Record/Call/",
                "/storage/emulated/0/Recordings/Call/",
                "/storage/emulated/0/CallRecordings/",
                "/storage/emulated/0/Call/",
                "/storage/emulated/0/PhoneRecord/",
                "/storage/emulated/0/MIUI/sound_recorder/call_rec/",
                "/storage/emulated/0/Recordings/",
                "/storage/emulated/0/Vivo/callrecord/",
                "/storage/emulated/0/record/callrec/",
                "/storage/emulated/0/Sounds/",
                "/storage/emulated/0/AudioRecorder/",
                "/storage/emulated/0/ColorOS/Recorder/Call/",
                "/storage/emulated/0/Recordings/Call Records/"
            )

            // Ghép priority + fallback, loại trùng lặp, giữ thứ tự
            val allDirs = (priorityDirs + fallbackDirs).distinct()

            // Chỉ giữ thư mục thực sự tồn tại trên máy này
            val existingDirs = allDirs.filter { File(it).exists() }

            Log.d(TAG, "📂 Thư mục sẽ quét (${existingDirs.size}/${allDirs.size} tồn tại):")
            existingDirs.forEachIndexed { i, dir ->
                val priority = if (i < priorityDirs.size) "★ " else "  "
                Log.d(TAG, "   $priority[$i] $dir")
            }

            // Nếu không có thư mục nào tồn tại, vẫn trả về toàn bộ để đảm bảo quét
            return if (existingDirs.isNotEmpty()) existingDirs else allDirs
        }
    }


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val state = when (stateStr) {
            TelephonyManager.EXTRA_STATE_IDLE -> TelephonyManager.CALL_STATE_IDLE
            TelephonyManager.EXTRA_STATE_OFFHOOK -> TelephonyManager.CALL_STATE_OFFHOOK
            TelephonyManager.EXTRA_STATE_RINGING -> TelephonyManager.CALL_STATE_RINGING
            else -> return
        }

        onCallStateChanged(context, state, number)
    }

    private fun onCallStateChanged(context: Context, state: Int, number: String?) {
        if (lastState == state) return

        when (state) {

            TelephonyManager.CALL_STATE_RINGING -> {
                savedNumber = number
                callAnswered = false
                Log.d(TAG, "📲 Đang đổ chuông từ: $number")
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Gọi đi thì EXTRA_INCOMING_NUMBER thường null — số sẽ lấy từ CallLog sau
                if (savedNumber == null) savedNumber = number
                callAnswered = true
                Log.d(TAG, "📞 Cuộc gọi bắt đầu | Số: ${savedNumber ?: "đang gọi đi"}")
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                val wasConnected =
                    (lastState == TelephonyManager.CALL_STATE_OFFHOOK) && callAnswered
                if (wasConnected) {
                    Log.d(
                        TAG,
                        "🔴 Cuộc gọi kết thúc | Đợi ${SCAN_DELAY_MS / 1000}s rồi quét file..."
                    )
                    triggerScanAfterDelay(context)
                } else {
                    Log.d(TAG, "🔕 Cuộc gọi nhỡ hoặc không bắt máy — bỏ qua.")
                }
                savedNumber = null
                callAnswered = false
            }
        }

        lastState = state
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bước 2: Đợi 3 giây rồi quét thư mục
    // ─────────────────────────────────────────────────────────────────────────

    private fun triggerScanAfterDelay(context: Context) {
        Handler(Looper.getMainLooper()).postDelayed({
            val recordedFile = scanForNewRecording()

            if (recordedFile != null) {
                // ✅ Bước 3: Tìm thấy file — nhân viên tuân thủ đúng quy trình
                Log.i(TAG, "✅ Tìm thấy file ghi âm: ${recordedFile.absolutePath}")
                showSuccessNotification(context, recordedFile)
                notifyAppToRefreshFiles(context)
                enqueueUpload(context, recordedFile.absolutePath)
            } else {
                // ❌ Bước 4: Không tìm thấy — hiển thị màn hình cảnh báo
                Log.w(TAG, "⚠️ KHÔNG tìm thấy file ghi âm → Hiển thị cảnh báo!")
                showWarning(context)
            }
        }, SCAN_DELAY_MS)
    }

    private fun notifyAppToRefreshFiles(context: Context) {
        try {
            val intent = Intent(ACTION_REFRESH_FILES).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "📢 Đã phát thông báo cập nhật danh sách ghi âm cho UI")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phát thông báo refresh: ${e.message}")
        }
    }

    private fun showSuccessNotification(context: Context, file: File) {
        try {
            val sizeKb = file.length() / 1024
            val msg = "File: ${file.name} ($sizeKb KB)\nĐường dẫn: ${file.parent}"

            Toast.makeText(context, "✅ Đã ghi nhận file ghi âm: ${file.name}", Toast.LENGTH_LONG)
                .show()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val channelId = "TelesalesServiceChannel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Telesales Service Channel",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager?.createNotificationChannel(channel)
            }

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("✅ Đã ghi nhận file ghi âm cuộc gọi")
                .setContentText("File: ${file.name} ($sizeKb KB)")
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(msg))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi hiển thị thông báo ghi nhận file: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bước 3: Quét tất cả thư mục phổ biến
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Quét danh sách thư mục để tìm file .mp3/.amr/.wav được tạo trong
     * vòng [FILE_FRESHNESS_MS] ms tính đến thời điểm quét.
     *
     * @return File đầu tiên tìm thấy, hoặc null nếu không có.
     */
    private fun scanForNewRecording(): File? {
        val now = System.currentTimeMillis()
        val threshold = now - FILE_FRESHNESS_MS - SCAN_DELAY_MS

        // Định dạng file ghi âm phổ biến của ứng dụng tích hợp Samsung, MIUI, OPPO...
        val validExtensions = setOf("mp3", "amr", "wav", "m4a", "3gp", "aac")

        Log.d(
            TAG,
            "🔍 Bắt đầu quét | Ngưỡng thời gian: ${formatTime(threshold)} → ${formatTime(now)}"
        )

        for (dirPath in getDirectoriesForDevice()) {
            val dir = File(dirPath)
            if (!dir.exists() || !dir.isDirectory) continue

            Log.d(TAG, "   📂 Đang kiểm tra: $dirPath")

            val excludeKeywords = listOf(
                "viber", "whatsapp", "telegram", "ringtone", "notification", "alarm",
                "over_the_horizon", "over the horizon", "overthehorizon", "horizon",
                "podcasts", "ui/audio", "notifications", "ringtones", "alarms", "sec_music"
            )

            val newFile = dir.listFiles()
                ?.filter { file ->
                    val pathLower = file.absolutePath.lowercase()
                    val nameLower = file.name.lowercase()
                    file.isFile &&
                            file.extension.lowercase() in validExtensions &&
                            file.lastModified() >= threshold &&
                            !excludeKeywords.any { pathLower.contains(it) || nameLower.contains(it) }
                }
                ?.maxByOrNull { it.lastModified() } // Lấy file mới nhất nếu có nhiều

            if (newFile != null) {
                Log.d(
                    TAG,
                    "   ✅ Phát hiện file tại [$dirPath]: ${newFile.name} | ${formatTime(newFile.lastModified())}"
                )
                return newFile
            }
        }

        Log.w(TAG, "🚫 Không tìm thấy file ghi âm mới trong tất cả thư mục đã quét.")
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bước 4: Hiển thị màn hình cảnh báo toàn màn hình
    // ─────────────────────────────────────────────────────────────────────────

    private fun showWarning(context: Context) {
        try {
            // 1. Toast cảnh báo khẩn cấp
            Toast.makeText(
                context,
                "🚨 CẢNH BÁO VI PHẠM: Không tìm thấy file ghi âm cuộc gọi!",
                Toast.LENGTH_LONG
            ).show()

            // 2. Phát âm thanh chuông báo động & Rung khẩn cấp ngay lập tức
            playEmergencyAlarmSound(context)

            // 3. Tạo PendingIntent đến WarningActivity
            val warningIntent = Intent(context, WarningActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            } else {
                android.app.PendingIntent.FLAG_UPDATE_CURRENT
            }
            val fullScreenPendingIntent = android.app.PendingIntent.getActivity(
                context,
                1001,
                warningIntent,
                pendingIntentFlags
            )

            // 4. Bắn Thông báo khẩn cấp với FullScreenIntent (Ép Android 10-14 hiển thị đè màn hình)
            showWarningNotification(context, fullScreenPendingIntent)

            // 5. Thử mở trực tiếp Activity
            try {
                context.startActivity(warningIntent)
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Mở startActivity bị hệ thống Android hạn chế, FullScreenIntent sẽ đảm nhận: ${e.message}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi hiển thị màn hình cảnh báo WarningActivity: ${e.message}")
        }
    }

    private fun playEmergencyAlarmSound(context: Context) {
        try {
            val toneG = android.media.ToneGenerator(android.media.AudioManager.STREAM_ALARM, 100)
            toneG.startTone(android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(
                        1000,
                        android.os.VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(1000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi phát âm thanh cảnh báo ngầm: ${e.message}")
        }
    }

    private fun showWarningNotification(
        context: Context,
        fullScreenPendingIntent: android.app.PendingIntent
    ) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
            val channelId = "TelesalesWarningChannel_v2"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Telesales Cảnh Báo Khẩn Cấp",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Kênh thông báo cảnh báo tuân thủ quy trình ghi âm cuộc gọi"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                }
                notificationManager?.createNotificationChannel(channel)
            }

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🚨 CẢNH BÁO VI PHẠM TUÂN THỦ GHI ÂM")
                .setContentText("Không tìm thấy file ghi âm cuộc gọi vừa rồi!")
                .setStyle(
                    androidx.core.app.NotificationCompat.BigTextStyle()
                        .bigText("CẢNH BÁO: Cuộc gọi vừa kết thúc KHÔNG CÓ file ghi âm!\n\nVui lòng mở Cài đặt ứng dụng Điện thoại ➔ Ghi âm cuộc gọi ➔ Bật tính năng Tự động ghi âm để tiếp tục làm việc.")
                )
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MAX)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager?.notify(9999, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi tạo thông báo cảnh báo: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Upload
    // ─────────────────────────────────────────────────────────────────────────

    private fun enqueueUpload(context: Context, filePath: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
        }
        val callAtFormatted = sdf.format(Date())

        val inputData = androidx.work.Data.Builder()
            .putString(UploadAudioWorker.KEY_FILE_PATH, filePath)
            .putString(UploadAudioWorker.KEY_PHONE_TO, savedNumber)
            .putString(UploadAudioWorker.KEY_CALL_TYPE, "outgoing")
            .putString(UploadAudioWorker.KEY_CALL_AT, callAtFormatted)
            .build()

        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val request = androidx.work.OneTimeWorkRequestBuilder<UploadAudioWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                "Telesales_Upload_Queue",
                androidx.work.ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )

        Log.d(TAG, "📤 Đã xếp hàng upload file ($callAtFormatted): $filePath")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private fun formatTime(ms: Long): String =
        SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(ms))
}
