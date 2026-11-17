package com.example.scentcast

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Asks you to rate how strong a perfume still smells, 1, 3 and 6 hours after you log a wear.
 * Each reminder is an alarm that fires a [ReminderReceiver], which shows a notification.
 */
object Reminders {
    private val HOURS = listOf(1, 3, 6)
    private const val CHANNEL_ID = "longevity"

    /** In demo mode the hours become minutes, so the reminders can be shown in class. */
    private fun delayMillis(hours: Int): Long =
        hours * (if (Settings.demoReminders) 60_000L else 3_600_000L)

    fun schedule(context: Context, wear: WearLog) {
        val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (hours in HOURS) {
            val intent = Intent(context, ReminderReceiver::class.java)
                .putExtra("wearId", wear.id)
                .putExtra("name", wear.perfumeName)
                .putExtra("hours", hours)
            val pending = PendingIntent.getBroadcast(
                context,
                wear.id * 10 + hours, // unique per wear and per reminder
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            // Inexact alarm: needs no special permission, may arrive a little late.
            alarms.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + delayMillis(hours),
                pending
            )
        }
    }

    fun show(context: Context, name: String, hours: Int) {
        val canNotify = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!canNotify) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        // Tapping the notification opens the diary, where the rating is given.
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, DiaryActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.reminder_title, name))
            .setContentText(context.getString(R.string.reminder_text, hours))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(name.hashCode() + hours, notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("name") ?: return
        Reminders.show(context, name, intent.getIntExtra("hours", 1))
    }
}
