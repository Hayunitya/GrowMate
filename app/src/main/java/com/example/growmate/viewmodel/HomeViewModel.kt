package com.example.growmate.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.growmate.model.Plant

class HomeViewModel : ViewModel() {
    private val _plantList = MutableLiveData<List<Plant>>()
    val plantList: LiveData<List<Plant>> = _plantList

    fun loadPlants() {
        _plantList.value = listOf(
            Plant("Tomat", "Sayur", "2025-07-10"),
            Plant("Bayam", "Sayur", "2025-07-08")
        )
    }
}