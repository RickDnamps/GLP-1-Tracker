package com.rickdnamps.glp1tracker

import android.app.Application
import com.rickdnamps.glp1tracker.data.AppDatabase
import com.rickdnamps.glp1tracker.data.SettingsRepository
import com.rickdnamps.glp1tracker.data.TrackerRepository
import com.rickdnamps.glp1tracker.health.HealthConnect
import com.rickdnamps.glp1tracker.notifications.Notifications
import com.rickdnamps.glp1tracker.scale.BleScaleManager
import com.rickdnamps.glp1tracker.widget.WidgetUpdater

class Glp1App : Application() {
    val repository: TrackerRepository by lazy {
        TrackerRepository(AppDatabase.create(this), SettingsRepository(this))
    }

    val scaleManager: BleScaleManager by lazy { BleScaleManager(this) }
    val healthConnect: HealthConnect by lazy { HealthConnect(this) }

    /** Called after data changes made outside the UI (notification actions, widget). */
    fun onDataChanged() {
        WidgetUpdater.requestRefresh(this)
    }

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
    }
}
