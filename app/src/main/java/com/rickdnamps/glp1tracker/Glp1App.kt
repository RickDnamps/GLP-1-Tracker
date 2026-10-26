package com.rickdnamps.glp1tracker

import android.app.Application
import com.rickdnamps.glp1tracker.data.AppDatabase
import com.rickdnamps.glp1tracker.data.SettingsRepository
import com.rickdnamps.glp1tracker.data.TrackerRepository
import com.rickdnamps.glp1tracker.notifications.Notifications

class Glp1App : Application() {
    val repository: TrackerRepository by lazy {
        TrackerRepository(AppDatabase.create(this), SettingsRepository(this))
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}
