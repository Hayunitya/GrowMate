package com.example.growmate

import com.example.growmate.R
import android.app.AlertDialog
import android.app.DatePickerDialog
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
import java.util.*

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

        // DatePicker konsisten format dd-MM-yy
        binding.etPlantDate.setOnClickListener { showDatePicker(binding.etPlantDate) }

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
            if (date.isEmpty()) {
                Toast.makeText(context, "Tanggal tanam wajib diisi", Toast.LENGTH_SHORT).show()
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
                        // Setelah update, ambil data terbaru untuk set alarm lagi!
                        FirebaseFirestore.getInstance().collection("plants").document(id).get()
                            .addOnSuccessListener { doc ->
                                val updatedName = doc.getString("plantName") ?: ""
                                val updatedWaterFreq = doc.getString("wateringFrequency") ?: ""
                                val updatedFertilizerFreq = doc.getString("fertilizingFrequency") ?: ""

                                // Set ulang alarm watering
                                val wateringTimes = getTimesForFrequency(updatedWaterFreq)
                                val wateringInterval = getIntervalMillis(updatedWaterFreq)
                                for (time in wateringTimes) {
                                    AlarmHelper.setRepeatingAlarm(
                                        requireContext(),
                                        plantId = updatedName + "_WATER",
                                        plantName = updatedName,
                                        type = "WATERING",
                                        hour = time.first,
                                        minute = time.second,
                                        intervalMillis = wateringInterval
                                    )
                                }
                                // Set ulang alarm fertilizing
                                val fertilizingTimes = getTimesForFrequency(updatedFertilizerFreq)
                                val fertilizingInterval = getIntervalMillis(updatedFertilizerFreq)
                                for (time in fertilizingTimes) {
                                    AlarmHelper.setRepeatingAlarm(
                                        requireContext(),
                                        plantId = updatedName + "_FERTILIZE",
                                        plantName = updatedName,
                                        type = "FERTILIZING",
                                        hour = time.first,
                                        minute = time.second,
                                        intervalMillis = fertilizingInterval
                                    )
                                }
                                Toast.makeText(context, "Berhasil diupdate", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Gagal set ulang alarm!", Toast.LENGTH_SHORT).show()
                                findNavController().popBackStack()
                            }
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

    // DatePicker dd-MM-yy (dua digit tahun)
    private fun showDatePicker(target: View) {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            (target as? android.widget.EditText)?.setText(String.format("%02d-%02d-%02d", d, m + 1, y % 100))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun getTimesForFrequency(freq: String): List<Pair<Int, Int>> {
        return when (freq) {
            "2 kali sehari" -> listOf(6 to 0, 18 to 0)
            "1 kali sehari" -> listOf(18 to 0)
            "2 hari sekali" -> listOf(18 to 0)
            "3 hari sekali" -> listOf(18 to 0)
            "7 hari sekali" -> listOf(18 to 0)
            "2 minggu sekali" -> listOf(18 to 0)
            "sebulan sekali" -> listOf(18 to 0)
            else -> listOf(18 to 0)
        }
    }

    private fun getIntervalMillis(freq: String): Long {
        return when (freq) {
            "2 kali sehari" -> (12 * 60 * 60 * 1000L)
            "1 kali sehari" -> (24 * 60 * 60 * 1000L)
            "2 hari sekali" -> (2 * 24 * 60 * 60 * 1000L)
            "3 hari sekali" -> (3 * 24 * 60 * 60 * 1000L)
            "7 hari sekali" -> (7 * 24 * 60 * 60 * 1000L)
            "2 minggu sekali" -> (14 * 24 * 60 * 60 * 1000L)
            "sebulan sekali" -> (30 * 24 * 60 * 60 * 1000L)
            else -> (24 * 60 * 60 * 1000L)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
