package com.example.growmate.repository

import com.example.growmate.model.Plant
import com.google.firebase.firestore.FirebaseFirestore

class PlantRepository {

    private val db = FirebaseFirestore.getInstance()
    private val plantRef = db.collection("plants")

    fun addPlant(plant: Plant, onResult: (Boolean, String?) -> Unit) {
        val docId = plantRef.document().id
        val plantWithId = plant.copy(id = docId)
        plantRef.document(docId).set(plantWithId)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun getPlantsByUser(userId: String, onResult: (List<Plant>?, String?) -> Unit) {
        plantRef.whereEqualTo("userId", userId).get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Plant::class.java)
                onResult(list, null)
            }
            .addOnFailureListener { onResult(null, it.message) }
    }

    fun updatePlant(plantId: String, updatedData: Map<String, Any>, onResult: (Boolean, String?) -> Unit) {
        plantRef.document(plantId).update(updatedData)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }

    fun deletePlant(plantId: String, onResult: (Boolean, String?) -> Unit) {
        plantRef.document(plantId).delete()
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.message) }
    }
}
