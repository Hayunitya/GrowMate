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
    // Tidak disimpan di Firestore (opsional)
    var waterStreak: Int = 0,
    var fertilizerStreak: Int = 0,
    var lastWateredDate: String = "",      // yyyy-MM-dd
    var lastFertilizedDate: String = "",   // yyyy-MM-dd
    val wateringFrequency: String = "",
    val fertilizingFrequency: String = "",
    // ✅ Tambahan Baru (pastikan ini ada di Firestore juga)
    var waterStatus: String = "Sudah Disiram",
    var fertilizerStatus: String = "Sudah Dipupuk",
    // Points (masih optional)
    var points: Int = 0
)
