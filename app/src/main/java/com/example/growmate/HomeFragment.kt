package com.example.growmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.growmate.adapter.PlantAdapter
import com.example.growmate.databinding.FragmentHomeBinding
import com.example.growmate.model.Plant
import com.example.growmate.viewmodel.PlantViewModel
import com.google.firebase.auth.FirebaseAuth

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        plantViewModel = ViewModelProvider(this)[PlantViewModel::class.java]

        val userId = auth.currentUser?.uid ?: return

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
                    putString("plantType", plant.plantType)
                    putString("plantDate", plant.plantDate)
                    putString("location", plant.location)
                    putString("notes", plant.notes)
                    putString("nextWateringDate", plant.nextWateringDate)
                }
                findNavController().navigate(R.id.action_homeFragment_to_editFragment, bundle)
            }
        )

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
