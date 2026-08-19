package com.nhakhoaquangninh.telesales.call

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nhakhoaquangninh.telesales.CallStateReceiver
import com.nhakhoaquangninh.telesales.MainActivity
import com.nhakhoaquangninh.telesales.R
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
            "Đã ghi nhận thông tin cuộc gọi và lưu vào bộ nhớ đệm cục bộ (Cache) để tự động đồng bộ."
        )

        ensureServiceChannel()
        val pendingIntent = createOpenAppPendingIntent()
        runCatching {
            notificationManager?.notify(
                System.currentTimeMillis().toInt(),
                NotificationCompat.Builder(appContext, SERVICE_CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(appContext.getString(R.string.notification_call_saved_title))
                    .setContentText(appContext.getString(R.string.notification_call_saved_content))
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setAutoCancel(true)
                    .build()
            )
        }
        notifyHistoryChanged()
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

    private companion object {
        const val SERVICE_CHANNEL_ID = "TelesalesServiceChannel"
        const val NEEDS_REVIEW_NOTIFICATION_ID = 9998
    }
}
