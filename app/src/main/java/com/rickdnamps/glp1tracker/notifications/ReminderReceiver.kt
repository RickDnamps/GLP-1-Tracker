package com.rickdnamps.glp1tracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rickdnamps.glp1tracker.Glp1App
import com.rickdnamps.glp1tracker.R
import com.rickdnamps.glp1tracker.domain.Format
import com.rickdnamps.glp1tracker.domain.Medication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        val repository = (context.applicationContext as Glp1App).repository
        when (intent.action) {
            ACTION_REMIND -> runAsync {
                if (ReminderScheduler.isDoseDue(repository)) {
                    val s = repository.settingsRepository.current()
                    val medName = if (s.medication == Medication.OTHER && s.customMedName.isNotBlank()) {
                        s.customMedName
                    } else {
                        context.getString(s.medication.nameRes)
                    }
                    Notifications.showDoseReminder(
                        context,
                        context.getString(R.string.notif_title),
                        context.getString(R.string.notif_text, medName, Format.dose(s.doseMg)),
                    )
                }
                ReminderScheduler.reschedule(context, repository)
            }
            ACTION_MARK_DONE -> runAsync {
                repository.logPlannedDose(System.currentTimeMillis())
                Notifications.cancelDoseReminder(context)
                ReminderScheduler.reschedule(context, repository)
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.rickdnamps.glp1tracker.action.REMIND"
        const val ACTION_MARK_DONE = "com.rickdnamps.glp1tracker.action.MARK_DONE"
    }
}

/** Alarms are cleared on reboot / time change, so schedule them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = (context.applicationContext as Glp1App).repository
        runAsync { ReminderScheduler.reschedule(context, repository) }
    }
}
