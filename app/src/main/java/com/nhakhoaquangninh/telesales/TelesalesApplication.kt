package com.nhakhoaquangninh.telesales

import android.app.Application
import com.nhakhoaquangninh.telesales.core.FileLogger
import com.nhakhoaquangninh.telesales.data.local.TokenManager

class TelesalesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        DailyCallCleanupScheduler.schedule(this)

        val userId = TokenManager.getInstance(this).getUserId()
        if (userId > 0) {
            FileLogger.setUserId(userId.toString())
        }
    }
}
