package com.example.growmate.model

data class Plant(
    val id: String = "",
    val userId: String = "",
    val plantName: String = "",
    val plantType: String = "",
    val plantDate: String = "",
    val location: String = "",
    val notes: String = "",
    val nextWateringDate: String = "",
    val nextFertilizingDate: String = "",
    var waterStreak: Int = 0,
    var fertilizerStreak: Int = 0,
    var lastWateredDate: String = "",
    var lastFertilizedDate: String = "",
    val wateringFrequency: String = "",
    val fertilizingFrequency: String = "",
    var waterStatus: String = "Sudah Disiram",
    var fertilizerStatus: String = "Sudah Dipupuk",
    var points: Int = 0
)
