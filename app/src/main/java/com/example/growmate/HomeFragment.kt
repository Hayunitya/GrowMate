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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var plantViewModel: PlantViewModel
    private lateinit var plantAdapter: PlantAdapter
    private val auth = FirebaseAuth.getInstance()

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

        val displayName = auth.currentUser?.displayName ?: "Pengguna"
        binding.tvGreeting.text = "Halo, $displayName!"

        plantAdapter = PlantAdapter(
            plants = listOf(),
            onDelete = { plant ->
                plantViewModel.deletePlant(plant.id) { success, _ ->
                    if (success) {
                        plantViewModel.fetchPlants(userId)
                    }
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
                val today = java.time.LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val lastWateredDate = try {
                    java.time.LocalDate.parse(plant.lastWateredDate, formatter)
                } catch (e: Exception) {
                    today.minusDays(10) // fallback biar streak reset
                }

                val newStreak = if (today.minusDays(1) == lastWateredDate) {
                    plant.streak + 1
                } else {
                    1
                }

                val updates = mapOf(
                    "lastWateredDate" to today.format(formatter),
                    "nextWateringDate" to today.plusDays(1).format(formatter),
                    "streak" to newStreak
                )

                FirebaseFirestore.getInstance()
                    .collection("plants")
                    .document(plant.id)
                    .update(updates)
                    .addOnSuccessListener {
                        plantViewModel.fetchPlants(userId)
                        Toast.makeText(requireContext(), "Tanaman disiram!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Gagal update", Toast.LENGTH_SHORT).show()
                    }
            }
        )


        binding.ivProfile.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_profileFragment)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = plantAdapter
        }

        binding.swipeRefresh.setOnRefreshListener {
            plantViewModel.fetchPlants(userId)
            binding.swipeRefresh.isRefreshing = false
        }

        plantViewModel.fetchPlants(userId)
        plantViewModel.plantList.observe(viewLifecycleOwner) { plants ->
            plantAdapter.updateData(plants)
            binding.tvEmpty.visibility = if (plants.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
