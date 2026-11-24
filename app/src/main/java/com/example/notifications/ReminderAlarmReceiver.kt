package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.repository.ReminderRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_ID) ?: return

        when (action) {
            ReminderNotificationHelper.ACTION_SHOW_REMINDER -> {
                val title = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_TITLE)
                    ?: "Cadence Reminder"
                val notes = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_NOTES)
                    ?: ""
                val cadence = intent.getStringExtra(ReminderNotificationHelper.EXTRA_REMINDER_CADENCE)
                    ?: "Scheduled"

                ReminderNotificationHelper.showReminderNotification(
                    context = context,
                    reminderId = reminderId,
                    title = title,
                    notes = notes,
                    cadenceLabel = cadence
                )

                if (Firebase.auth.currentUser != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            val repository = ReminderRepository(context.applicationContext)
                            repository.fetchRemindersOnce().onSuccess { list ->
                                list.firstOrNull { it.id == reminderId }?.let { reminder ->
                                    val next = reminder.nextTriggerMillis(System.currentTimeMillis() + 60_000L)
                                    if (next != null) {
                                        ReminderNotificationHelper.scheduleAlarm(
                                            context.applicationContext,
                                            reminder,
                                            next
                                        )
                                    }
                                }
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ReminderNotificationHelper.ACTION_COMPLETE_REMINDER -> {
                NotificationManagerCompat.from(context).cancel(reminderId.hashCode())
                if (Firebase.auth.currentUser != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            val repository = ReminderRepository(context.applicationContext)
                            repository.markCompleteById(reminderId)
                            repository.fetchRemindersOnce().onSuccess { list ->
                                ReminderNotificationHelper.syncAllAlarms(
                                    context.applicationContext,
                                    list
                                )
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            ReminderNotificationHelper.ACTION_SNOOZE_REMINDER -> {
                NotificationManagerCompat.from(context).cancel(reminderId.hashCode())
                val snoozeUntil = System.currentTimeMillis() + 60L * 60L * 1000L
                if (Firebase.auth.currentUser != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            val repository = ReminderRepository(context.applicationContext)
                            repository.snoozeReminder(reminderId, snoozeUntil)
                            repository.fetchRemindersOnce().onSuccess { list ->
                                ReminderNotificationHelper.syncAllAlarms(
                                    context.applicationContext,
                                    list
                                )
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
