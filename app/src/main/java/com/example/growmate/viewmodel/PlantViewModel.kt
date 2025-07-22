package com.example.growmate.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.growmate.model.Plant
import com.example.growmate.repository.PlantRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PlantViewModel : ViewModel() {

    private val plantRepo = PlantRepository()

    private val _plantList = MutableLiveData<List<Plant>>()
    val plantList: LiveData<List<Plant>> get() = _plantList

    fun fetchPlants(userId: String) {
        plantRepo.getPlantsByUser(userId) { list, _ ->
            list?.let { _plantList.value = it }
        }
    }

    fun addPlant(plant: Plant, onResult: (Boolean, String?) -> Unit) {
        plantRepo.addPlant(plant, onResult)
    }

    fun updatePlant(id: String, data: Map<String, Any>, onResult: (Boolean, String?) -> Unit) {
        plantRepo.updatePlant(id, data, onResult)
    }

    fun deletePlant(id: String, onResult: (Boolean, String?) -> Unit) {
        plantRepo.deletePlant(id, onResult)
    }

    // Fungsi untuk update penyiraman
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateWatering(plant: Plant, onResult: (Boolean, String?) -> Unit) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val nextExpected = LocalDate.parse(plant.nextWateringDate, formatter)

        var newStreak = plant.waterStreak
        var newPoints = plant.points

        if (!plant.lastWateredDate.equals(today.format(formatter))) {
            // Tepat waktu atau lebih cepat
            if (today.isEqual(nextExpected) || today.isBefore(nextExpected)) {
                newStreak += 1
                newPoints += 10
            } else {
                newStreak = 0
                newPoints += 3
            }

            val data = mapOf(
                "lastWateredDate" to today.format(formatter),
                "waterStreak" to newStreak,
                "points" to newPoints,
                "waterStatus" to "Sudah Disiram"
            )

            updatePlant(plant.id, data, onResult)
        } else {
            onResult(false, "Tanaman sudah disiram hari ini.")
        }
    }

    // Fungsi untuk update pemupukan
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateFertilizing(plant: Plant, onResult: (Boolean, String?) -> Unit) {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val nextExpected = LocalDate.parse(plant.nextFertilizingDate, formatter)

        var newStreak = plant.fertilizerStreak
        var newPoints = plant.points

        if (!plant.lastFertilizedDate.equals(today.format(formatter))) {
            // Tepat waktu atau lebih cepat
            if (today.isEqual(nextExpected) || today.isBefore(nextExpected)) {
                newStreak += 1
                newPoints += 15  // lebih besar dari siram
            } else {
                newStreak = 0
                newPoints += 5
            }

            val data = mapOf(
                "lastFertilizedDate" to today.format(formatter),
                "fertilizerStreak" to newStreak,
                "points" to newPoints,
                "fertilizerStatus" to "Sudah Dipupuk"
            )

            updatePlant(plant.id, data, onResult)
        } else {
            onResult(false, "Tanaman sudah dipupuk hari ini.")
        }
    }
}
