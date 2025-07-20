package com.example.growmate

import android.app.NotificationChannel
import android.app.NotificationManager
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
                            updateData["waterStreak"] = 0  // Reset streak kalau telat
                        }
                        updateData["waterStatus"] = "Belum Disiram"
                    }

                    if (type == "FERTILIZING") {
                        val lastFertilized = doc.getString("lastFertilizedDate") ?: today.minusDays(10).format(formatter)
                        val lastDate = LocalDate.parse(lastFertilized, formatter)

                        if (lastDate.isBefore(today)) {
                            updateData["fertilizerStreak"] = 0  // Reset streak kalau telat
                        }
                        updateData["fertilizerStatus"] = "Belum Dipupuk"
                    }

                    doc.reference.update(updateData)
                }
            }
            .addOnFailureListener {
                Log.e("ReminderReceiver", "Gagal update status: ${it.message}")
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
            .setSmallIcon(R.drawable.logo_growmate)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify((plantName + type).hashCode(), notification)
        Log.d("ReminderReceiver", "Notifikasi & update status + streak check untuk $plantName tipe $type sukses!")
    }
}

