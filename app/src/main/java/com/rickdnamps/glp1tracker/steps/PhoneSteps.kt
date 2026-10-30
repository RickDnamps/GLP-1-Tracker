package com.rickdnamps.glp1tracker.steps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.rickdnamps.glp1tracker.domain.StepMath
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate

/**
 * Built-in pedometer: the phone's hardware step counter (counts even when the app is closed,
 * reset at reboot). Readings are turned into steps per day by [StepMath] and kept locally.
 */
object PhoneSteps {
    const val PERMISSION = Manifest.permission.ACTIVITY_RECOGNITION

    private const val PREFS = "phone_steps"

    private fun sensorManager(context: Context) = context.getSystemService(SensorManager::class.java)

    fun isSupported(context: Context): Boolean = sensorManager(context)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(context, PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun load(context: Context): StepMath.State? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!p.contains("day")) return null
        val history = p.getString("history", "").orEmpty().split(',').mapNotNull { part ->
            val (d, s) = part.split(':').takeIf { it.size == 2 } ?: return@mapNotNull null
            d.toLongOrNull()?.let { day -> s.toLongOrNull()?.let { day to it } }
        }.toMap()
        return StepMath.State(p.getLong("day", 0), p.getLong("base", 0), p.getLong("last", 0), history)
    }

    @Synchronized
    private fun record(context: Context, counter: Long): StepMath.State {
        val state = StepMath.update(load(context), LocalDate.now().toEpochDay(), counter)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong("day", state.day)
            .putLong("base", state.base)
            .putLong("last", state.last)
            .putString("history", state.history.entries.joinToString(",") { "${it.key}:${it.value}" })
            .apply()
        return state
    }

    /** Raw counter values while collected. */
    private fun counter(context: Context): Flow<Long> = callbackFlow {
        val manager = sensorManager(context)
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (manager == null || sensor == null || !hasPermission(context)) {
            close()
            return@callbackFlow
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                trySend(event.values[0].toLong())
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { manager.unregisterListener(listener) }
    }

    /** The stored state right away, then live updates as steps are taken (while collected). */
    fun updates(context: Context): Flow<StepMath.State> = flow {
        load(context)?.let { emit(it) }
        emitAll(counter(context).map { record(context, it) })
    }

    /** One reading in the background (reminders, boot) so days are closed even if the app stays shut. */
    suspend fun sample(context: Context, timeoutMs: Long = 3_000) {
        if (!isSupported(context) || !hasPermission(context)) return
        withTimeoutOrNull(timeoutMs) { counter(context).first() }?.let { record(context, it) }
    }
}
