package com.nhakhoaquangninh.telesales

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TelesalesForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_service_title))
            .setContentText(getString(R.string.notification_service_content))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        _isRunning.value = true
        _isMonitoring.value = com.nhakhoaquangninh.telesales.data.local.TokenManager.getInstance(this).isMonitoringEnabled()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        _isMonitoring.value = false
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_service_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
        }
        getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(serviceChannel)
    }

    companion object {
        private const val CHANNEL_ID = "TelesalesServiceChannel"
        private const val NOTIFICATION_ID = 1

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isMonitoring = MutableStateFlow(false)
        val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

        fun setMonitoring(enabled: Boolean) {
            _isMonitoring.value = enabled
        }

        fun startService(context: android.content.Context) {
            try {
                val serviceIntent = Intent(context.applicationContext, TelesalesForegroundService::class.java)
                androidx.core.content.ContextCompat.startForegroundService(context.applicationContext, serviceIntent)
            } catch (e: Exception) {
                android.util.Log.e("TelesalesService", "Không thể khởi chạy Foreground Service: ${e.message}")
            }
        }
    }
}