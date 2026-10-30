package com.rickdnamps.glp1tracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rickdnamps.glp1tracker.MainActivity
import com.rickdnamps.glp1tracker.R

object Notifications {
    const val CHANNEL_REMINDERS = "dose_reminders"
    const val CHANNEL_HABITS = "habit_reminders"
    const val ID_DOSE = 1001

    fun createChannels(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = context.getString(R.string.notif_channel_description) }
        val habits = NotificationChannel(
            CHANNEL_HABITS,
            context.getString(R.string.notif_habits_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notif_habits_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(channel, habits))
    }

    /** Gentle reminders (weigh-in, water, journal, eve of the dose). */
    fun showHabit(context: Context, type: ReminderType, title: String, text: String, actions: List<Pair<String, PendingIntent>> = emptyList()) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 100 + type.requestCode,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_HABITS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .apply { actions.forEach { (label, intent) -> addAction(0, label, intent) } }
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2000 + type.requestCode, notification)
        } catch (_: SecurityException) {
        }
    }

    fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33

    fun showDoseReminder(context: Context, title: String, text: String) {
        if (!canPost(context)) return
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val done = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_MARK_DONE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notif_action_done), done)
            .addAction(
                0, context.getString(R.string.notif_action_snooze),
                PendingIntent.getBroadcast(
                    context, 2,
                    Intent(context, ReminderReceiver::class.java).setAction(ReminderReceiver.ACTION_SNOOZE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
        try {
            NotificationManagerCompat.from(context).notify(ID_DOSE, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    fun cancelHabit(context: Context, type: ReminderType) {
        NotificationManagerCompat.from(context).cancel(2000 + type.requestCode)
    }

    fun cancelDoseReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_DOSE)
    }
}
