package com.example.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.ReminderEntity

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: ReminderEntity) {
        if (reminder.dueTimestamp <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_REMINDER_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_REMINDER_NOTES, reminder.notes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueTimestamp,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueTimestamp,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback for devices restricting exact alarms
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminder.dueTimestamp,
                pendingIntent
            )
        }
    }

    fun cancel(reminderId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
