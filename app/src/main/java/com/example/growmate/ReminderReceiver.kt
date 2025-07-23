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
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val today = LocalDate.now()

                for (doc in snapshot) {
                    val updateData = mutableMapOf<String, Any>()

                    // Ambil status & reminderCount lama
                    val waterStatus = doc.getString("waterStatus") ?: "Belum Disiram"
                    val fertilizerStatus = doc.getString("fertilizerStatus") ?: "Belum Dipupuk"
                    val reminderCount = (doc.getLong("reminderCount") ?: 0).toInt()
                    val wateringFreq = doc.getString("wateringFrequency") ?: ""
                    val fertilizingFreq = doc.getString("fertilizingFrequency") ?: ""

                    // Apakah reminder harus dikirim? (kecuali 2x/1x sehari)
                    val isReminderException =
                        (type == "WATERING" && (wateringFreq == "2 kali sehari" || wateringFreq == "1 kali sehari")) ||
                                (type == "FERTILIZING" && (fertilizingFreq == "2 kali sehari" || fertilizingFreq == "1 kali sehari"))

                    // RESET STREAK jika notif kedua muncul & status masih belum berubah
                    if (type == "WATERING") {
                        if (waterStatus == "Belum Disiram" && reminderCount >= 1) {
                            updateData["waterStreak"] = 0
                            Log.d("ReminderReceiver", "Reset water streak for $plantName (reminderCount=$reminderCount)")
                        }
                        updateData["waterStatus"] = "Belum Disiram"
                    }
                    if (type == "FERTILIZING") {
                        if (fertilizerStatus == "Belum Dipupuk" && reminderCount >= 1) {
                            updateData["fertilizerStreak"] = 0
                            Log.d("ReminderReceiver", "Reset fertilizer streak for $plantName (reminderCount=$reminderCount)")
                        }
                        updateData["fertilizerStatus"] = "Belum Dipupuk"
                    }

                    // Kirim reminder kalau status belum berubah & bukan 2x/1x sehari
                    if (!isReminderException && (
                                (type == "WATERING" && waterStatus == "Belum Disiram") ||
                                        (type == "FERTILIZING" && fertilizerStatus == "Belum Dipupuk"))
                    ) {
                        updateData["reminderCount"] = reminderCount + 1
                        sendReminderNotification(context, plantName, type)
                    } else {
                        updateData["reminderCount"] = 0 // reset jika sudah disiram/dipupuk
                    }

                    doc.reference.update(updateData)
                        .addOnSuccessListener {
                            Log.d("ReminderReceiver", "Firestore updated successfully for $plantName")
                        }
                        .addOnFailureListener { e ->
                            Log.e("ReminderReceiver", "Firestore update failed: ${e.message}")
                        }

                    // Kirim notifikasi utama (alarm) hanya jika ini alarm utama (bukan reminder)
                    if (reminderCount == 0) {
                        sendMainNotification(context, plantName, type)
                    }
                }
            }
            .addOnFailureListener {
                Log.e("ReminderReceiver", "Firestore query failed: ${it.message}")
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendMainNotification(context: Context, plantName: String, type: String) {
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
            (plantName + type).hashCode(),
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
        Log.d("ReminderReceiver", "Main notification sent for $plantName - $type")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendReminderNotification(context: Context, plantName: String, type: String) {
        val title = when (type) {
            "WATERING" -> "Pengingat Tambahan Penyiraman"
            "FERTILIZING" -> "Pengingat Tambahan Pemupukan"
            else -> "Pengingat Tambahan Perawatan"
        }
        val content = when (type) {
            "WATERING" -> "Tanaman $plantName belum disiram. Segera siram sekarang!"
            "FERTILIZING" -> "Tanaman $plantName belum dipupuk. Segera pupuk sekarang!"
            else -> "Segera rawat tanaman $plantName!"
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("PLANT_NAME", plantName)
            putExtra("TYPE", type)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (plantName + type + "reminder").hashCode(),
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

        notificationManager.notify((plantName + type + "reminder").hashCode(), notification)
        Log.d("ReminderReceiver", "Reminder notification sent for $plantName - $type")
    }
}
