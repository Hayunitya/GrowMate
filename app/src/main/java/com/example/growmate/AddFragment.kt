package com.example.growmate

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
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

        binding.etPlantDate.setOnClickListener { showDatePicker(binding.etPlantDate) }
        binding.etNextWateringDate.setOnClickListener { showDatePicker(binding.etNextWateringDate) }

        binding.btnSave.setOnClickListener {
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

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val plant = Plant("", userId, name, type, date, location, notes, nextWatering)
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
        binding.etPlantType.text?.clear()
        binding.etPlantDate.text?.clear()
        binding.etLocation.text?.clear()
        binding.etNotes.text?.clear()
        binding.etNextWateringDate.text?.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
