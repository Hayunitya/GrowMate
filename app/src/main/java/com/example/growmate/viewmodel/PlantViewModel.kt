package com.example.growmate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.growmate.model.Plant
import com.example.growmate.repository.PlantRepository

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
}
