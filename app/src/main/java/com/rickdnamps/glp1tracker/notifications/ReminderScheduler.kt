package com.rickdnamps.glp1tracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rickdnamps.glp1tracker.data.TrackerRepository
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Schedule
import java.time.LocalDate
import java.time.LocalDateTime

object ReminderScheduler {

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_REMIND),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    /** Recomputes the next reminder from the current settings and last logged dose. */
    suspend fun reschedule(context: Context, repository: TrackerRepository) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(context)
        am.cancel(pi)

        val settings = repository.settingsRepository.current()
        if (!settings.remindersEnabled || !settings.onboardingDone) return

        val now = LocalDateTime.now()
        val last = repository.latestInjection()?.let { Format.toLocalDateTime(it.timestamp) }
        val due = Schedule.nextDue(last, settings.intervalDays, settings.injectionDay, settings.reminderTime, now.toLocalDate())
        val fireAt = Schedule.nextReminder(due, settings.reminderTime, now)
        val triggerMillis = Format.toMillis(fireAt)

        if (canScheduleExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pi)
        }
    }

    /** True if a dose is due today or overdue (the alarm may fire slightly early or late). */
    suspend fun isDoseDue(repository: TrackerRepository): Boolean {
        val settings = repository.settingsRepository.current()
        val now = LocalDateTime.now()
        val last = repository.latestInjection()?.let { Format.toLocalDateTime(it.timestamp) }
        val due = Schedule.nextDue(last, settings.intervalDays, settings.injectionDay, settings.reminderTime, now.toLocalDate())
        return !due.toLocalDate().isAfter(LocalDate.now())
    }
}
