package com.example.growmate

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var plantViewModel: PlantViewModel
    private lateinit var plantAdapter: PlantAdapter
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    // TAMBAH: Simpan semua tanaman di sini
    private var allPlants: List<Plant> = listOf()

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

        fetchUserPoints()

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
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val nextExpected = try {
                    LocalDate.parse(plant.nextWateringDate, formatter)
                } catch (e: Exception) {
                    today
                }

                val (newStreak, pointGain) = if (today.isEqual(nextExpected) || today.isBefore(nextExpected)) {
                    plant.waterStreak + 1 to 10
                } else {
                    0 to 3
                }

                val updates = mapOf(
                    "lastWateredDate" to today.format(formatter),
                    "nextWateringDate" to today.plusDays(1).format(formatter),
                    "waterStreak" to newStreak,
                    "waterStatus" to "Sudah Disiram",
                    "reminderCount" to 0
                )

                val db = FirebaseFirestore.getInstance()
                val plantDoc = db.collection("plants").document(plant.id)
                val userDoc = db.collection("users").document(auth.currentUser!!.uid)

                db.runBatch { batch ->
                    batch.update(plantDoc, updates)
                    batch.update(userDoc, "points", FieldValue.increment(pointGain.toLong()))
                }.addOnSuccessListener {
                    Toast.makeText(requireContext(), "Tanaman disiram! (+$pointGain poin)", Toast.LENGTH_SHORT).show()
                    fetchUserPoints()
                }.addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal update siram", Toast.LENGTH_SHORT).show()
                }
            },
            onFertilized = { plant ->
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
                val nextExpected = try {
                    LocalDate.parse(plant.nextFertilizingDate, formatter)
                } catch (e: Exception) {
                    today
                }

                val (newStreak, pointGain) = if (today.isEqual(nextExpected) || today.isBefore(nextExpected)) {
                    plant.fertilizerStreak + 1 to 15
                } else {
                    0 to 5
                }

                val updates = mapOf(
                    "lastFertilizedDate" to today.format(formatter),
                    "nextFertilizingDate" to today.plusDays(7).format(formatter),
                    "fertilizerStreak" to newStreak,
                    "fertilizerStatus" to "Sudah Dipupuk",
                    "reminderCount" to 0
                )

                val db = FirebaseFirestore.getInstance()
                val plantDoc = db.collection("plants").document(plant.id)
                val userDoc = db.collection("users").document(auth.currentUser!!.uid)

                db.runBatch { batch ->
                    batch.update(plantDoc, updates)
                    val pointUpdate = mapOf("points" to FieldValue.increment(pointGain.toLong()))
                    batch.set(userDoc, pointUpdate, SetOptions.merge())
                }.addOnSuccessListener {
                    Toast.makeText(requireContext(), "Tanaman dipupuk! (+$pointGain poin)", Toast.LENGTH_SHORT).show()
                    fetchUserPoints()
                }.addOnFailureListener {
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

        // Tambahkan TextWatcher untuk search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim()?.lowercase() ?: ""
                val filtered = if (query.isEmpty()) {
                    allPlants
                } else {
                    allPlants.filter { plant ->
                        plant.plantName.lowercase().contains(query)
                        // Bisa tambahkan filter berdasarkan notes/status, dll di sini
                    }
                }
                plantAdapter.updateData(filtered)
                binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        })

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

                val plantList = snapshot?.documents?.mapNotNull { doc ->
                    val plant = doc.toObject(Plant::class.java)
                    plant?.copy(id = doc.id)
                } ?: emptyList()

                allPlants = plantList // simpan semua tanaman
                val searchText = binding.etSearch.text?.toString()?.trim()?.lowercase() ?: ""
                val filtered = if (searchText.isEmpty()) {
                    allPlants
                } else {
                    allPlants.filter { plant ->
                        plant.plantName.lowercase().contains(searchText)
                    }
                }
                plantAdapter.updateData(filtered)
                binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun fetchUserPoints() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDoc = FirebaseFirestore.getInstance().collection("users").document(uid)

        userDoc.get()
            .addOnSuccessListener { snapshot ->
                val points = snapshot.getLong("points") ?: 0
                binding.tvPoints.text = "Poin: $points"
            }
            .addOnFailureListener {
                binding.tvPoints.text = "Poin: -"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listenerRegistration?.remove()
        _binding = null
    }
}
