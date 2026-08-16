package com.nhakhoaquangninh.telesales.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.nhakhoaquangninh.telesales.CallStateReceiver
import com.nhakhoaquangninh.telesales.MainActivity
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.WarningActivity
import com.nhakhoaquangninh.telesales.domain.model.CallRecordMetadata
import com.nhakhoaquangninh.telesales.domain.model.CallType

class ComplianceNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    fun notifyUploadSuccess(metadata: CallRecordMetadata) {
        ensureServiceChannel()
        val targetPhone = getDisplayPhone(metadata)
        val content = if (metadata.isAnswered) {
            appContext.getString(R.string.notification_upload_success_content, targetPhone)
        } else {
            appContext.getString(R.string.notification_upload_unanswered_success_content, targetPhone)
        }
        val pendingIntent = createOpenAppPendingIntent()
        val notificationId = getUploadNotificationId(metadata)

        runCatching {
            notificationManager?.notify(
                notificationId,
                NotificationCompat.Builder(appContext, SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_upload_success_title))
                    .setContentText(content)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    fun notifyUploadFailed(metadata: CallRecordMetadata, failureReason: String?) {
        ensureServiceChannel()
        val targetPhone = getDisplayPhone(metadata)
        val content = if (failureReason == "unauthorized") {
            appContext.getString(R.string.notification_upload_failed_unauthorized)
        } else {
            appContext.getString(R.string.notification_upload_failed_content, targetPhone)
        }
        val pendingIntent = createOpenAppPendingIntent()
        val notificationId = getUploadNotificationId(metadata)

        runCatching {
            notificationManager?.notify(
                notificationId,
                NotificationCompat.Builder(appContext, SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_upload_failed_title))
                    .setContentText(content)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    private fun getDisplayPhone(metadata: CallRecordMetadata): String {
        return when (metadata.callType) {
            CallType.INCOMING -> metadata.phoneNumberFrom ?: metadata.phoneNumberTo
            else -> metadata.phoneNumberTo ?: metadata.phoneNumberFrom
        } ?: appContext.getString(R.string.history_unknown_phone)
    }

    private fun getUploadNotificationId(metadata: CallRecordMetadata): Int {
        val rawKey = metadata.recordingUri
            ?: "${metadata.callAtFormatted}_${metadata.phoneNumberTo ?: metadata.phoneNumberFrom}"
        return rawKey.hashCode() and 0x7FFFFFFF
    }

    private fun createOpenAppPendingIntent(): PendingIntent {
        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            appContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun notifyRecordingQueued() {
        ensureServiceChannel()
        notificationManager?.notify(
            System.currentTimeMillis().toInt(),
            NotificationCompat.Builder(appContext, SERVICE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(appContext.getString(R.string.notification_call_saved_title))
                .setContentText(appContext.getString(R.string.notification_call_saved_content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .build()
        )
        notifyHistoryChanged()
    }

    fun notifyNeedsReview() {
        ensureServiceChannel()
        notificationManager?.notify(
            NEEDS_REVIEW_NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, SERVICE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(appContext.getString(R.string.notification_review_title))
                .setContentText(appContext.getString(R.string.notification_review_content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .build()
        )
        notifyHistoryChanged()
    }

    fun notifyMissingRecording() {
        com.nhakhoaquangninh.telesales.core.FileLogger.log(
            appContext,
            "COMPLIANCE",
            "🚨 Phát hiện cuộc gọi có thời lượng đàm thoại nhưng THIẾU FILE GHI ÂM (Vi phạm quy định telesales). Đang kích hoạt WarningActivity..."
        )

        // Hiển thị Toast trên Main Thread an toàn
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                Toast.makeText(
                    appContext,
                    appContext.getString(R.string.notification_warning_toast),
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                android.util.Log.w("ComplianceNotifier", "Không thể hiển thị Toast: ${e.message}")
            }
        }

        val warningIntent = Intent(appContext, WarningActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        // 1. Tự động mở WarningActivity trực tiếp đè lên màn hình
        val startDirectSuccess = runCatching {
            appContext.startActivity(warningIntent)
            true
        }.getOrElse { e ->
            android.util.Log.e("ComplianceNotifier", "Không thể tự động mở WarningActivity trực tiếp: ${e.message}")
            com.nhakhoaquangninh.telesales.core.FileLogger.log(
                appContext,
                "COMPLIANCE",
                "⚠️ Không thể mở WarningActivity trực tiếp (có thể do thiếu quyền Overlay hoặc bị OEM chặn): ${e.message}"
            )
            false
        }

        if (startDirectSuccess) {
            com.nhakhoaquangninh.telesales.core.FileLogger.log(
                appContext,
                "COMPLIANCE",
                "✅ Đã gọi startActivity(WarningActivity) thành công!"
            )
        }

        // 2. Dự phòng: Đẩy Notification mức khẩn cấp cao nhất kèm FullScreenIntent
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            WARNING_NOTIFICATION_ID,
            warningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        ensureWarningChannel()
        runCatching {
            notificationManager?.notify(
                WARNING_NOTIFICATION_ID,
                NotificationCompat.Builder(appContext, WARNING_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_warning_title))
                    .setContentText(appContext.getString(R.string.notification_warning_content))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(appContext.getString(R.string.notification_warning_detail))
                    )
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pendingIntent, true)
                    .setContentIntent(pendingIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true)
                    .build()
            )
        }
    }

    fun notifyHistoryChanged() {
        appContext.sendBroadcast(
            Intent(CallStateReceiver.ACTION_REFRESH_FILES).setPackage(appContext.packageName)
        )
    }

    private fun ensureServiceChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    appContext.getString(R.string.notification_service_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE }
            )
        }
    }

    private fun ensureWarningChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager?.createNotificationChannel(
                NotificationChannel(
                    WARNING_CHANNEL_ID,
                    appContext.getString(R.string.notification_warning_channel),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = appContext.getString(R.string.notification_warning_channel_desc)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                }
            )
        }
    }

    private companion object {
        const val SERVICE_CHANNEL_ID = "TelesalesServiceChannel"
        const val WARNING_CHANNEL_ID = "TelesalesWarningChannel_v2"
        const val WARNING_NOTIFICATION_ID = 9999
        const val NEEDS_REVIEW_NOTIFICATION_ID = 9998
    }
}
