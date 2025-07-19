package com.example.growmate

import com.example.growmate.R
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.growmate.databinding.FragmentEditBinding
import com.example.growmate.viewmodel.PlantViewModel
import com.google.firebase.firestore.FirebaseFirestore

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

        // Inisialisasi opsi spinner
        val freqList = listOf(
            "Pilih frekuensi",
            "2 kali sehari", "1 kali sehari", "2 hari sekali", "3 hari sekali",
            "7 hari sekali", "2 minggu sekali", "sebulan sekali"
        )

        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, freqList)
        binding.spinnerWaterFreq.setAdapter(adapter)
        binding.spinnerFertilizerFreq.setAdapter(adapter)

        val args = arguments
        plantId = args?.getString("plantId")
        binding.etPlantName.setText(args?.getString("plantName") ?: "")
        binding.etPlantDate.setText(args?.getString("plantDate") ?: "")
        binding.etNotes.setText(args?.getString("notes") ?: "")

        // Set posisi pilihan dropdown
        val wateringFreq = args?.getString("wateringFreq")
        val fertilizingFreq = args?.getString("fertilizingFreq")

        binding.spinnerWaterFreq.setText(wateringFreq, false)
        binding.spinnerFertilizerFreq.setText(fertilizingFreq, false)

        binding.btnUpdate.setOnClickListener {
            val name = binding.etPlantName.text.toString().trim()
            val date = binding.etPlantDate.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()
            val waterFreq = binding.spinnerWaterFreq.text.toString().trim()
            val fertilizerFreq = binding.spinnerFertilizerFreq.text.toString().trim()


            if (name.isEmpty()) {
                Toast.makeText(context, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedFreq = binding.spinnerWaterFreq.text.toString()
            if (selectedFreq == "Pilih frekuensi") {
                Toast.makeText(context, "Frekuensi penyiraman belum dipilih", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updateMap = mapOf(
                "plantName" to name,
                "plantDate" to date,
                "notes" to notes,
                "wateringFrequency" to waterFreq,
                "fertilizingFrequency" to fertilizerFreq
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
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Hapus Tanaman")
                .setMessage("Yakin ingin menghapus tanaman ini?")
                .setPositiveButton("Ya") { _, _ ->
                    val id = arguments?.getString("plantId") ?: return@setPositiveButton
                    FirebaseFirestore.getInstance()
                        .collection("plants")
                        .document(id)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Tanaman dihapus", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack() // Balik ke home
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal hapus tanaman", Toast.LENGTH_SHORT).show()
                        }
                }
                .setNegativeButton("Batal", null)
                .show()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
