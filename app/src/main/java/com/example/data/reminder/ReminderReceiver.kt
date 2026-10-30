package com.example.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "solynext_reminders_channel"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_NOTES = "extra_reminder_notes"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, 0L)
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "SolyNext Reminder"
        val notes = intent.getStringExtra(EXTRA_REMINDER_NOTES) ?: "Scheduled alert from SolyNext AI"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SolyNext Assistant Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts and reminders scheduled with SolyNext AI Assistant"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_solynext_logo)
            .setContentTitle("SolyNext: $title")
            .setContentText(notes)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)

        // Mark notified in database
        if (reminderId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    db.reminderDao().markNotified(reminderId)
                } catch (_: Exception) {}
            }
        }
    }
}
