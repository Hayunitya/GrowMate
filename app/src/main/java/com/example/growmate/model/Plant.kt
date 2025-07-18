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
    // Tambahan (tidak disimpan di Firestore)
    var streak: Int = 0,
    var lastWateredDate: String = "",      // "yyyy-MM-dd"
    val wateringFrequency: String = "",
    val fertilizingFrequency: String = "",

)