package com.rickdnamps.glp1tracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rickdnamps.glp1tracker.Glp1App
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.data.TrackerRepository
import com.rickdnamps.glp1tracker.data.UserSettings
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Medication
import com.rickdnamps.glp1tracker.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
private val doneMutex = Mutex()

/** Runs [block] off the main thread while keeping the broadcast alive. */
private fun BroadcastReceiver.runAsync(block: suspend () -> Unit) {
    val pending = goAsync()
    receiverScope.launch {
        try {
            block()
        } finally {
            pending.finish()
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as Glp1App
        val repository = app.repository
        when (intent.action) {
            ACTION_REMIND -> runAsync {
                val type = ReminderType.fromName(intent.getStringExtra(EXTRA_TYPE)) ?: ReminderType.DOSE
                remind(context, repository, type)
                ReminderScheduler.reschedule(context, repository)
                app.onDataChanged()
            }
            ACTION_MARK_DONE -> runAsync {
                // Remove the button first so a double tap cannot fire twice, then log only if still due.
                Notifications.cancelDoseReminder(context)
                doneMutex.withLock {
                    if (ReminderScheduler.isDoseDue(repository)) repository.logPlannedDose(System.currentTimeMillis())
                }
                ReminderScheduler.reschedule(context, repository)
                app.onDataChanged()
            }
        }
    }

    private suspend fun remind(context: Context, repository: TrackerRepository, type: ReminderType) {
        val s = repository.settingsRepository.current()
        when (type) {
            ReminderType.DOSE -> if (ReminderScheduler.isDoseDue(repository)) {
                Notifications.showDoseReminder(
                    context,
                    context.getString(R.string.notif_title),
                    context.getString(R.string.notif_text, medName(context, s), Format.dose(s.doseMg)),
                )
            }
            ReminderType.DOSE_EVE -> Notifications.showHabit(
                context, type,
                context.getString(R.string.notif_eve_title),
                context.getString(R.string.notif_eve_text, medName(context, s), Format.dose(s.doseMg)),
            )
            ReminderType.WEIGH_IN -> Notifications.showHabit(
                context, type,
                context.getString(R.string.notif_weigh_title),
                context.getString(R.string.notif_weigh_text),
            )
            ReminderType.WATER -> {
                val water = repository.dailyLog(LocalDate.now())?.waterMl ?: 0
                if (water < s.waterGoalMl) {
                    Notifications.showHabit(
                        context, type,
                        context.getString(R.string.notif_water_title),
                        context.getString(R.string.notif_water_text, water, s.waterGoalMl),
                    )
                }
            }
            ReminderType.JOURNAL -> {
                val log = repository.dailyLog(LocalDate.now())
                if (log == null || (log.mood == 0 && log.appetite == 0)) {
                    Notifications.showHabit(
                        context, type,
                        context.getString(R.string.notif_journal_title),
                        context.getString(R.string.notif_journal_text),
                    )
                }
            }
        }
    }

    private fun medName(context: Context, s: UserSettings): String =
        if (s.medication == Medication.OTHER && s.customMedName.isNotBlank()) s.customMedName
        else context.getString(s.medication.nameRes)

    companion object {
        const val ACTION_REMIND = "com.rickdnamps.glp1tracker.action.REMIND"
        const val ACTION_MARK_DONE = "com.rickdnamps.glp1tracker.action.MARK_DONE"
        const val EXTRA_TYPE = "type"
    }
}

/** Alarms are cleared on reboot / time change, so schedule them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as Glp1App).repository
        runAsync {
            ReminderScheduler.reschedule(context, repository)
            WidgetUpdater.refresh(context)
        }
    }
}
