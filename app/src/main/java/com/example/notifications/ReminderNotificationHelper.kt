package com.example.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.Reminder

object ReminderNotificationHelper {
    const val CHANNEL_ID = "cadence_reminders"
    const val ACTION_SHOW_REMINDER = "com.example.action.SHOW_REMINDER"
    const val ACTION_COMPLETE_REMINDER = "com.example.action.COMPLETE_REMINDER"
    const val ACTION_SNOOZE_REMINDER = "com.example.action.SNOOZE_REMINDER"

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
    const val EXTRA_REMINDER_NOTES = "extra_reminder_notes"
    const val EXTRA_REMINDER_CADENCE = "extra_reminder_cadence"

    private const val TAG = "ReminderNotifications"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun syncAllAlarms(context: Context, reminders: List<Reminder>) {
        ensureChannel(context)
        val now = System.currentTimeMillis()
        reminders.forEach { reminder ->
            if (reminder.id.isBlank()) return@forEach
            val triggerAt = reminder.nextTriggerMillis(now)
            if (triggerAt != null && triggerAt > now) {
                scheduleAlarm(context, reminder, triggerAt)
            } else {
                cancelAlarm(context, reminder.id)
            }
        }
    }

    fun scheduleAlarm(context: Context, reminder: Reminder, triggerAtMillis: Long) {
        if (reminder.id.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildShowPendingIntent(context, reminder)
        try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Falling back to inexact alarm: ${e.message}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, reminderId: String) {
        if (reminderId.isBlank()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showReminderNotification(
        context: Context,
        reminderId: String,
        title: String,
        notes: String,
        cadenceLabel: String
    ) {
        ensureChannel(context)
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "POST_NOTIFICATIONS not granted; skipping notification for $reminderId")
            return
        }

        val notifId = reminderId.hashCode()
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_COMPLETE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 10001,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_NOTES, notes)
            putExtra(EXTRA_REMINDER_CADENCE, cadenceLabel)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId + 20002,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (notes.isNotBlank()) "$cadenceLabel · $notes" else "$cadenceLabel reminder"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)

        if (reminderId != "test_notification") {
            builder.addAction(
                0,
                context.getString(R.string.action_mark_done),
                donePendingIntent
            )
            builder.addAction(
                0,
                context.getString(R.string.action_snooze_1h),
                snoozePendingIntent
            )
        }

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to show notification: ${e.message}")
        }
    }

    fun showTestNotification(context: Context) {
        showReminderNotification(
            context = context,
            reminderId = "test_notification",
            title = "Cadence Reminders Active",
            notes = "Your daily, monthly, and yearly alerts are ready.",
            cadenceLabel = "Test"
        )
    }

    private fun buildShowPendingIntent(context: Context, reminder: Reminder): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_SHOW_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminder.id)
            putExtra(EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(EXTRA_REMINDER_NOTES, reminder.notes)
            putExtra(EXTRA_REMINDER_CADENCE, reminder.cadenceType().label)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
