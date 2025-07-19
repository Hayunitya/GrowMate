package com.example.growmate

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmHelper {

    fun setRepeatingAlarm(
        context: Context,
        plantId: String,
        plantName: String,
        type: String,
        hour: Int,
        minute: Int,
        intervalMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("PLANT_ID", plantId)
            putExtra("PLANT_NAME", plantName)
            putExtra("TYPE", type)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (plantId + hour + minute + type).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            intervalMillis,
            pendingIntent
        )
    }

    fun cancelAlarm(
        context: Context,
        plantId: String,
        type: String,
        hour: Int,
        minute: Int
    ) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (plantId + hour + minute + type).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}
