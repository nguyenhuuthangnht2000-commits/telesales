package com.nhakhoaquangninh.telesales

import android.app.Application

class TelesalesApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
