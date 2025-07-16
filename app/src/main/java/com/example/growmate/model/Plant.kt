package com.example.growmate.model

data class Plant(
    val id: String = "",
    val userId: String = "",
    val plantName: String = "",
    val plantType: String = "",
    val plantDate: String = "",
    val location: String = "",
    val notes: String = "",
    val nextWateringDate: String = ""
)
