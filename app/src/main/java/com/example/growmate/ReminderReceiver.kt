package com.example.growmate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val plantName = intent.getStringExtra("PLANT_NAME") ?: "Tanaman"
        val type = intent.getStringExtra("TYPE") ?: "WATERING"

        val title = when (type) {
            "WATERING" -> "Pengingat Penyiraman"
            "FERTILIZING" -> "Pengingat Pemupukan"
            else -> "Pengingat Perawatan"
        }

        val content = when (type) {
            "WATERING" -> "Jangan lupa siram $plantName!"
            "FERTILIZING" -> "Saatnya memupuk $plantName!"
            else -> "Jangan lupa rawat $plantName!"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "growmate_reminder_channel",
                "GrowMate Reminder",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, "growmate_reminder_channel")
            .setSmallIcon(R.drawable.logo_growmate)  // Ganti sesuai icon kamu
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify((plantName + type).hashCode(), notification)

        Log.d("ReminderReceiver", "Notifikasi terkirim untuk $plantName dengan tipe $type")

    }

}
