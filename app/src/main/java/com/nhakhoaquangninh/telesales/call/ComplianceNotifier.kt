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
import com.nhakhoaquangninh.telesales.R
import com.nhakhoaquangninh.telesales.WarningActivity

class ComplianceNotifier(context: Context) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

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
        Toast.makeText(
            appContext,
            appContext.getString(R.string.notification_warning_toast),
            Toast.LENGTH_LONG
        ).show()
        val warningIntent = Intent(appContext, WarningActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            WARNING_NOTIFICATION_ID,
            warningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        ensureWarningChannel()
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
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setAutoCancel(true)
                .build()
        )
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
