package com.example.growmate

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.growmate.adapter.PlantAdapter
import com.example.growmate.databinding.FragmentHomeBinding
import com.example.growmate.viewmodel.HomeViewModel

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PlantAdapter(emptyList())
        binding.plantRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.plantRecyclerView.adapter = adapter

        viewModel.plantList.observe(viewLifecycleOwner) { plants ->
            adapter.updateData(plants)
        }

        viewModel.loadPlants() // fetch dummy atau real data dari Firestore
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}