package com.nhakhoaquangninh.telesales

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat

class TelesalesForegroundService : Service() {

    private val CHANNEL_ID = "TelesalesServiceChannel"
    private val NOTIFICATION_ID = 1
    private lateinit var callStateReceiver: CallStateReceiver

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Đăng ký động CallStateReceiver (SIM call monitoring)
        callStateReceiver = CallStateReceiver()
        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(callStateReceiver, filter)
        // StringeeManager đã được loại bỏ — app không còn dùng VoIP
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telesales App Đang Hoạt Động")
            .setContentText("Ứng dụng đang chạy ngầm để ghi nhận cuộc gọi.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // START_STICKY để service tự khởi động lại nếu bị hệ thống kill
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy đăng ký receiver khi service bị tắt
        unregisterReceiver(callStateReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Telesales Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
