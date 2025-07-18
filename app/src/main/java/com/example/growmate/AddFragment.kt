package com.example.growmate

import com.example.growmate.R
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.growmate.databinding.FragmentAddBinding
import com.example.growmate.model.Plant
import com.example.growmate.viewmodel.PlantViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var plantViewModel: PlantViewModel
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        plantViewModel = ViewModelProvider(this)[PlantViewModel::class.java]

        // Inisialisasi Spinner
        val freqList = listOf(
            "Pilih frekuensi",
            "2 kali sehari", "1 kali sehari", "2 hari sekali", "3 hari sekali",
            "7 hari sekali", "2 minggu sekali", "sebulan sekali"
        )

        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, freqList)
        binding.spinnerWaterFreq.setAdapter(adapter)
        binding.spinnerFertilizerFreq.setAdapter(adapter)

        // Tanggal tanam pakai DatePicker
        binding.etPlantDate.setOnClickListener {
            showDatePicker(binding.etPlantDate)
        }

        // Tombol Simpan
        binding.btnSave.setOnClickListener {
            val name = binding.etPlantName.text.toString().trim()
            val plantDate = binding.etPlantDate.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()
            val waterFreq = binding.spinnerWaterFreq.text.toString().trim()
            val fertilizerFreq = binding.spinnerFertilizerFreq.text.toString().trim()

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            if (name.isEmpty() || plantDate.isEmpty()) {
                Toast.makeText(context, "Nama dan tanggal tanam wajib diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (waterFreq.isEmpty() || fertilizerFreq.isEmpty()) {
                Toast.makeText(context, "Pilih frekuensi penyiraman dan pemupukan", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val plant = Plant(
                id = "",
                userId = userId,
                plantName = name,
                plantDate = plantDate,
                notes = notes,
                wateringFrequency = waterFreq,
                fertilizingFrequency = fertilizerFreq,
                lastWateredDate = plantDate,
                nextWateringDate = plantDate,
                streak = 0
            )

            plantViewModel.addPlant(plant) { success, message ->
                if (success) {
                    Toast.makeText(context, "Berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    clearFields()
                } else {
                    Toast.makeText(context, "Gagal: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDatePicker(target: View) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            (target as? android.widget.EditText)?.setText(String.format("%02d-%02d-%04d", d, m + 1, y))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun clearFields() {
        binding.etPlantName.text?.clear()
        binding.etPlantDate.text?.clear()
        binding.etNotes.text?.clear()
        binding.spinnerWaterFreq.setText("", false)
        binding.spinnerFertilizerFreq.setText("", false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
