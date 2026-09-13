package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

data class ReminderSettings(
    val checkInEnabled: Boolean = true,
    val checkInHour: Int = 9,
    val checkInMinute: Int = 0,
    val checkOutEnabled: Boolean = true,
    val checkOutHour: Int = 17,
    val checkOutMinute: Int = 30,
    val timetableAlertsEnabled: Boolean = true,
    val taskDueAlertsEnabled: Boolean = true,
    val vibrateSoundEnabled: Boolean = true
) {
    val checkInTimeFormatted: String
        get() = formatTime(checkInHour, checkInMinute)

    val checkOutTimeFormatted: String
        get() = formatTime(checkOutHour, checkOutMinute)

    companion object {
        fun formatTime(hour: Int, minute: Int): String {
            val h = if (hour == 0 || hour == 12) 12 else hour % 12
            val amPm = if (hour < 12) "AM" else "PM"
            return "%02d:%02d %s".format(h, minute, amPm)
        }
    }
}

object NotificationHelper {
    const val CHANNEL_ID = "productivity_reminders_channel"
    const val CHANNEL_NAME = "Productivity Reminders & Alerts"
    const val CHANNEL_DESC = "Notifications for attendance check-in/out, lectures, meetings, and tasks"

    private const val PREFS_NAME = "productivity_notification_prefs"

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun getSettings(context: Context): ReminderSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSettings(
            checkInEnabled = prefs.getBoolean("check_in_enabled", true),
            checkInHour = prefs.getInt("check_in_hour", 9),
            checkInMinute = prefs.getInt("check_in_minute", 0),
            checkOutEnabled = prefs.getBoolean("check_out_enabled", true),
            checkOutHour = prefs.getInt("check_out_hour", 17),
            checkOutMinute = prefs.getInt("check_out_minute", 30),
            timetableAlertsEnabled = prefs.getBoolean("timetable_alerts_enabled", true),
            taskDueAlertsEnabled = prefs.getBoolean("task_due_alerts_enabled", true),
            vibrateSoundEnabled = prefs.getBoolean("vibrate_sound_enabled", true)
        )
    }

    fun saveSettings(context: Context, settings: ReminderSettings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("check_in_enabled", settings.checkInEnabled)
            .putInt("check_in_hour", settings.checkInHour)
            .putInt("check_in_minute", settings.checkInMinute)
            .putBoolean("check_out_enabled", settings.checkOutEnabled)
            .putInt("check_out_hour", settings.checkOutHour)
            .putInt("check_out_minute", settings.checkOutMinute)
            .putBoolean("timetable_alerts_enabled", settings.timetableAlertsEnabled)
            .putBoolean("task_due_alerts_enabled", settings.taskDueAlertsEnabled)
            .putBoolean("vibrate_sound_enabled", settings.vibrateSoundEnabled)
            .apply()
    }

    fun sendInstantNotification(
        context: Context,
        id: Int,
        title: String,
        body: String,
        subText: String = "Productivity Hub"
    ): Boolean {
        initNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(subText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        return try {
            val manager = NotificationManagerCompat.from(context)
            if (hasNotificationPermission(context)) {
                manager.notify(id, builder.build())
                true
            } else {
                false
            }
        } catch (e: SecurityException) {
            false
        }
    }
}
