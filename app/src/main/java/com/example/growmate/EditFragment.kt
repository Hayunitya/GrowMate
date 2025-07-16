package com.example.growmate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.growmate.databinding.FragmentEditBinding
import com.example.growmate.viewmodel.PlantViewModel

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var plantViewModel: PlantViewModel
    private var plantId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        plantViewModel = ViewModelProvider(this)[PlantViewModel::class.java]

        val args = arguments
        plantId = args?.getString("plantId")
        binding.etPlantName.setText(args?.getString("plantName"))
        binding.etPlantType.setText(args?.getString("plantType"))
        binding.etPlantDate.setText(args?.getString("plantDate"))
        binding.etLocation.setText(args?.getString("location"))
        binding.etNotes.setText(args?.getString("notes"))
        binding.etNextWateringDate.setText(args?.getString("nextWateringDate"))

        binding.btnUpdate.setOnClickListener {
            val name = binding.etPlantName.text.toString().trim()
            val type = binding.etPlantType.text.toString().trim()
            val date = binding.etPlantDate.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()
            val nextWatering = binding.etNextWateringDate.text.toString().trim()

            if (name.isEmpty() || type.isEmpty()) {
                Toast.makeText(context, "Nama & Jenis wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateMap = mapOf(
                "plantName" to name,
                "plantType" to type,
                "plantDate" to date,
                "location" to location,
                "notes" to notes,
                "nextWateringDate" to nextWatering
            )

            plantId?.let { id ->
                plantViewModel.updatePlant(id, updateMap) { success, message ->
                    if (success) {
                        Toast.makeText(context, "Berhasil diupdate", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    } else {
                        Toast.makeText(context, "Gagal: $message", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
