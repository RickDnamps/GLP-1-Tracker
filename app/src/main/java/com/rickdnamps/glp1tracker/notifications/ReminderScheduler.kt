package com.rickdnamps.glp1tracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rickdnamps.glp1tracker.data.TrackerRepository
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Schedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** Every reminder the app can post; [requestCode] keeps their alarms independent. */
enum class ReminderType(val requestCode: Int) {
    DOSE(0), DOSE_EVE(10), WEIGH_IN(11), WATER(12), JOURNAL(13);

    companion object {
        fun fromName(name: String?) = entries.firstOrNull { it.name == name }
        val WATER_HOURS = listOf(10, 13, 16, 19)
        val EVE_TIME: LocalTime = LocalTime.of(20, 0)
        val JOURNAL_TIME: LocalTime = LocalTime.of(21, 0)
    }
}

object ReminderScheduler {

    private fun pendingIntent(context: Context, type: ReminderType): PendingIntent = PendingIntent.getBroadcast(
        context, type.requestCode,
        Intent(context, ReminderReceiver::class.java)
            .setAction(ReminderReceiver.ACTION_REMIND)
            .putExtra(ReminderReceiver.EXTRA_TYPE, type.name),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canScheduleExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    private suspend fun lastDose(repository: TrackerRepository): LocalDateTime? =
        repository.latestInjection()?.let { Format.toLocalDateTime(it.timestamp) }

    private fun due(settings: UserSettings, last: LocalDateTime?, now: LocalDateTime): LocalDateTime =
        Schedule.nextDue(last, settings.intervalDays, settings.injectionDay, settings.reminderTime, now.toLocalDate(), settings.skippedThrough)

    /** Next fire time for [type], or null when that reminder is off. */
    fun nextTime(type: ReminderType, s: UserSettings, last: LocalDateTime?, now: LocalDateTime): LocalDateTime? {
        if (!s.onboardingDone) return null
        return when (type) {
            ReminderType.DOSE -> if (!s.remindersEnabled) null else Schedule.nextReminder(due(s, last, now), s.reminderTime, now)
            ReminderType.DOSE_EVE -> {
                if (!s.doseEveReminder || s.intervalDays < 2) return null
                val eve = due(s, last, now).toLocalDate().minusDays(1).atTime(ReminderType.EVE_TIME)
                eve.takeIf { it.isAfter(now) }
            }
            ReminderType.WEIGH_IN -> {
                if (!s.weighInReminder) return null
                val time = LocalTime.of(s.weighInHour.coerceIn(0, 23), 0)
                val candidate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(s.weighInDay)).atTime(time)
                if (candidate.isAfter(now)) candidate else candidate.plusWeeks(1)
            }
            ReminderType.WATER -> {
                if (!s.waterReminders) return null
                val today = now.toLocalDate()
                ReminderType.WATER_HOURS.map { today.atTime(it, 0) }.firstOrNull { it.isAfter(now) }
                    ?: today.plusDays(1).atTime(ReminderType.WATER_HOURS.first(), 0)
            }
            ReminderType.JOURNAL -> {
                if (!s.journalReminder) return null
                val t = now.toLocalDate().atTime(ReminderType.JOURNAL_TIME)
                if (t.isAfter(now)) t else t.plusDays(1)
            }
        }
    }

    /** Recomputes every reminder from the current settings and last logged dose. */
    suspend fun reschedule(context: Context, repository: TrackerRepository) {
        val am = context.getSystemService(AlarmManager::class.java)
        val settings = repository.settingsRepository.current()
        val now = LocalDateTime.now()
        val last = lastDose(repository)
        for (type in ReminderType.entries) {
            val pi = pendingIntent(context, type)
            am.cancel(pi)
            val at = nextTime(type, settings, last, now) ?: continue
            val millis = Format.toMillis(at)
            // Exact timing matters for the dose; habits can be batched by the system.
            if (type == ReminderType.DOSE && canScheduleExact(context)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        }
    }

    /** Re-posts the dose reminder in one hour ("Later" button). */
    fun snooze(context: Context, minutes: Long = 60) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 99,
            Intent(context, ReminderReceiver::class.java)
                .setAction(ReminderReceiver.ACTION_REMIND)
                .putExtra(ReminderReceiver.EXTRA_TYPE, ReminderType.DOSE.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = System.currentTimeMillis() + minutes * 60_000
        if (canScheduleExact(context)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    /** True if a dose is due today or overdue (the alarm may fire slightly early or late). */
    suspend fun isDoseDue(repository: TrackerRepository): Boolean {
        val settings = repository.settingsRepository.current()
        val now = LocalDateTime.now()
        return !due(settings, lastDose(repository), now).toLocalDate().isAfter(LocalDate.now())
    }
}
