package com.autoyt.uploader

import android.app.Application

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
        Scheduler.start(this)
    }
}
