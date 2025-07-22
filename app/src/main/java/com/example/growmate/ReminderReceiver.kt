package com.example.growmate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReminderReceiver : BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val plantName = intent.getStringExtra("PLANT_NAME") ?: return
        val type = intent.getStringExtra("TYPE") ?: return
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Log.d("ReminderReceiver", "Alarm Triggered ➔ Plant: $plantName | Type: $type | User: $userId")

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("plants")
            .whereEqualTo("userId", userId)
            .whereEqualTo("plantName", plantName)
            .get()
            .addOnSuccessListener { snapshot ->
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val today = LocalDate.now()

                for (doc in snapshot) {
                    val updateData = mutableMapOf<String, Any>()

                    if (type == "WATERING") {
                        val lastWatered = doc.getString("lastWateredDate") ?: today.minusDays(10).format(formatter)
                        val lastDate = LocalDate.parse(lastWatered, formatter)

                        if (lastDate.isBefore(today)) {
                            updateData["waterStreak"] = 0
                            Log.d("ReminderReceiver", "Streak WATER reset for $plantName")
                        }
                        updateData["waterStatus"] = "Belum Disiram"
                        Log.d("ReminderReceiver", "Water Status set to 'Belum Disiram' for $plantName")
                    }

                    if (type == "FERTILIZING") {
                        val lastFertilized = doc.getString("lastFertilizedDate") ?: today.minusDays(10).format(formatter)
                        val lastDate = LocalDate.parse(lastFertilized, formatter)

                        if (lastDate.isBefore(today)) {
                            updateData["fertilizerStreak"] = 0
                            Log.d("ReminderReceiver", "Streak FERTILIZER reset for $plantName")
                        }
                        updateData["fertilizerStatus"] = "Belum Dipupuk"
                        Log.d("ReminderReceiver", "Fertilizer Status set to 'Belum Dipupuk' for $plantName")
                    }

                    doc.reference.update(updateData)
                        .addOnSuccessListener {
                            Log.d("ReminderReceiver", "Firestore updated successfully for $plantName")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ReminderReceiver", "Firestore update failed: ${e.message}")
                        }
                }
            }
            .addOnFailureListener {
                Log.e("ReminderReceiver", "Firestore query failed: ${it.message}")
            }

        // Kirim Notifikasi
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

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("PLANT_NAME", plantName)
            putExtra("TYPE", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (plantName + type).hashCode(),  // Unique requestCode per plant & type
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            "growmate_reminder_channel",
            "GrowMate Reminder",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, "growmate_reminder_channel")
            .setSmallIcon(R.drawable.logo_growmate)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((plantName + type).hashCode(), notification)
        Log.d("ReminderReceiver", "Notification sent for $plantName - $type")
    }
}


