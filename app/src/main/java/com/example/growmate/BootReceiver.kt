package com.example.growmate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.growmate.model.Plant
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            Log.d("BootReceiver", "Device rebooted, fetching plants and setting alarms...")

            val userId = FirebaseAuth.getInstance().currentUser?.uid

            if (userId != null) {
                FirebaseFirestore.getInstance()
                    .collection("plants")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (doc in snapshot) {
                            val plant = doc.toObject(Plant::class.java)
                            setAlarmForPlantAgain(context, plant)
                        }
                        Log.d("BootReceiver", "Alarms rescheduled for ${snapshot.size()} plants")
                    }
                    .addOnFailureListener {
                        Log.e("BootReceiver", "Failed to fetch plants on boot: ${it.message}")
                    }
            } else {
                Log.d("BootReceiver", "No user logged in on boot, skipping alarm reset")
            }
        }
    }

    private fun setAlarmForPlantAgain(context: Context, plant: Plant) {
        val wateringTimes = getTimesForFrequency(plant.wateringFrequency)
        val wateringInterval = getIntervalMillis(plant.wateringFrequency)

        for (time in wateringTimes) {
            AlarmHelper.setRepeatingAlarm(
                context,
                plantId = plant.plantName + "_WATER",
                plantName = plant.plantName,
                type = "WATERING",
                hour = time.first,
                minute = time.second,
                intervalMillis = wateringInterval
            )
        }

        val fertilizingTimes = getTimesForFrequency(plant.fertilizingFrequency)
        val fertilizingInterval = getIntervalMillis(plant.fertilizingFrequency)

        for (time in fertilizingTimes) {
            AlarmHelper.setRepeatingAlarm(
                context,
                plantId = plant.plantName + "_FERTILIZE",
                plantName = plant.plantName,
                type = "FERTILIZING",
                hour = time.first,
                minute = time.second,
                intervalMillis = fertilizingInterval
            )
        }
    }

    private fun getTimesForFrequency(freq: String): List<Pair<Int, Int>> {
        return when (freq) {
            "2 kali sehari" -> listOf(6 to 0, 18 to 0)
            "1 kali sehari" -> listOf(18 to 0)
            "2 hari sekali" -> listOf(18 to 0)
            "3 hari sekali" -> listOf(18 to 0)
            "7 hari sekali" -> listOf(18 to 0)
            "2 minggu sekali" -> listOf(18 to 0)
            "sebulan sekali" -> listOf(18 to 0)
            else -> listOf(18 to 0)
        }
    }

    private fun getIntervalMillis(freq: String): Long {
        return when (freq) {
            "2 kali sehari" -> (12 * 60 * 60 * 1000L)
            "1 kali sehari" -> (24 * 60 * 60 * 1000L)
            "2 hari sekali" -> (2 * 24 * 60 * 60 * 1000L)
            "3 hari sekali" -> (3 * 24 * 60 * 60 * 1000L)
            "7 hari sekali" -> (7 * 24 * 60 * 60 * 1000L)
            "2 minggu sekali" -> (14 * 24 * 60 * 60 * 1000L)
            "sebulan sekali" -> (30 * 24 * 60 * 60 * 1000L)
            else -> (24 * 60 * 60 * 1000L)
        }
    }
}
