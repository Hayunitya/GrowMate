package com.example.growmate

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.growmate.adapter.PlantAdapter
import com.example.growmate.databinding.FragmentHomeBinding
import com.example.growmate.model.Plant
import com.example.growmate.viewmodel.PlantViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var plantViewModel: PlantViewModel
    private lateinit var plantAdapter: PlantAdapter
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        plantViewModel = ViewModelProvider(this)[PlantViewModel::class.java]
        val userId = auth.currentUser?.uid ?: return

        binding.tvGreeting.text = "Halo, ${auth.currentUser?.displayName ?: "Pengguna"}!"

        plantAdapter = PlantAdapter(
            plants = listOf(),
            onDelete = { plant ->
                plantViewModel.deletePlant(plant.id) { success, _ ->
                    if (success) plantViewModel.fetchPlants(userId)
                }
            },
            onEdit = { plant ->
                val bundle = Bundle().apply {
                    putString("plantId", plant.id)
                    putString("plantName", plant.plantName)
                    putString("plantDate", plant.plantDate)
                    putString("notes", plant.notes)
                    putString("wateringFreq", plant.wateringFrequency)
                    putString("fertilizingFreq", plant.fertilizingFrequency)
                }
                findNavController().navigate(R.id.action_homeFragment_to_editFragment, bundle)
            },
            onWatered = { plant ->
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val lastWateredDate = try {
                    LocalDate.parse(plant.lastWateredDate, formatter)
                } catch (e: Exception) {
                    today.minusDays(10)
                }

                val newWaterStreak = if (today.minusDays(1) == lastWateredDate) {
                    plant.waterStreak + 1
                } else {
                    1
                }

                val updates = mapOf(
                    "lastWateredDate" to today.format(formatter),
                    "nextWateringDate" to today.plusDays(1).format(formatter),
                    "waterStreak" to newWaterStreak,
                    "waterStatus" to "Sudah Disiram"
                )

                FirebaseFirestore.getInstance()
                    .collection("plants")
                    .document(plant.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Tanaman disiram!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal update siram", Toast.LENGTH_SHORT).show()
                    }
            },
            onFertilized = { plant ->
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val lastFertilizedDate = try {
                    LocalDate.parse(plant.lastFertilizedDate, formatter)
                } catch (e: Exception) {
                    today.minusDays(10)
                }

                val newFertilizerStreak = if (today.minusDays(7) == lastFertilizedDate) {
                    plant.fertilizerStreak + 1
                } else {
                    1
                }

                val updates = mapOf(
                    "lastFertilizedDate" to today.format(formatter),
                    "nextFertilizingDate" to today.plusDays(7).format(formatter),
                    "fertilizerStreak" to newFertilizerStreak,
                    "fertilizerStatus" to "Sudah Dipupuk"
                )

                FirebaseFirestore.getInstance()
                    .collection("plants")
                    .document(plant.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Tanaman dipupuk!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal update pupuk", Toast.LENGTH_SHORT).show()
                    }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = plantAdapter
        }

        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        binding.swipeRefresh.setOnRefreshListener {
            plantViewModel.fetchPlants(userId)
            binding.swipeRefresh.isRefreshing = false
        }

        listenToPlantRealtime(userId)
    }

    private fun listenToPlantRealtime(userId: String) {
        listenerRegistration?.remove()
        listenerRegistration = FirebaseFirestore.getInstance()
            .collection("plants")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Gagal mengambil data: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val plantList = snapshot?.toObjects(Plant::class.java) ?: emptyList()
                plantAdapter.updateData(plantList)
                binding.tvEmpty.visibility = if (plantList.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}

