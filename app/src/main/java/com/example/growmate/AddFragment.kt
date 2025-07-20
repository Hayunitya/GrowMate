package com.example.growmate

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

        initFrequencyDropdown()
        initDatePicker()

        binding.btnSave.setOnClickListener { savePlant() }
    }

    private fun initFrequencyDropdown() {
        val freqList = listOf(
            "Pilih frekuensi",
            "2 kali sehari", "1 kali sehari", "2 hari sekali", "3 hari sekali",
            "7 hari sekali", "2 minggu sekali", "sebulan sekali"
        )
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, freqList)
        binding.spinnerWaterFreq.setAdapter(adapter)
        binding.spinnerFertilizerFreq.setAdapter(adapter)
    }

    private fun initDatePicker() {
        binding.etPlantDate.setOnClickListener { showDatePicker(binding.etPlantDate) }
    }

    private fun savePlant() {
        val name = binding.etPlantName.text.toString().trim()
        val plantDate = binding.etPlantDate.text.toString().trim()
        val notes = binding.etNotes.text.toString().trim()
        val waterFreq = binding.spinnerWaterFreq.text.toString().trim()
        val fertilizerFreq = binding.spinnerFertilizerFreq.text.toString().trim()
        val userId = auth.currentUser?.uid ?: return

        if (name.isEmpty() || plantDate.isEmpty()) {
            Toast.makeText(context, "Nama dan tanggal tanam wajib diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (waterFreq == "Pilih frekuensi" || fertilizerFreq == "Pilih frekuensi") {
            Toast.makeText(context, "Pilih frekuensi penyiraman dan pemupukan", Toast.LENGTH_SHORT).show()
            return
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
            lastFertilizedDate = plantDate,
            nextFertilizingDate = plantDate,
            waterStreak = 0,
            fertilizerStreak = 0,
            waterStatus = "Belum Disiram",
            fertilizerStatus = "Belum Dipupuk"
        )


        plantViewModel.addPlant(plant) { success, message ->
            if (success) {
                Toast.makeText(context, "Berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                clearFields()
                setAlarmForPlant(plant)
            } else {
                Toast.makeText(context, "Gagal: $message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setAlarmForPlant(plant: Plant) {
        // Alarm Penyiraman
        val wateringTimes = getTimesForFrequency(plant.wateringFrequency)
        val wateringInterval = getIntervalMillis(plant.wateringFrequency)

        for (time in wateringTimes) {
            AlarmHelper.setRepeatingAlarm(
                requireContext(),
                plantId = plant.plantName + "_WATER",
                plantName = plant.plantName,
                type = "WATERING",
                hour = time.first,
                minute = time.second,
                intervalMillis = wateringInterval
            )
        }

        // Alarm Pemupukan
        val fertilizingTimes = getTimesForFrequency(plant.fertilizingFrequency)
        val fertilizingInterval = getIntervalMillis(plant.fertilizingFrequency)

        for (time in fertilizingTimes) {
            AlarmHelper.setRepeatingAlarm(
                requireContext(),
                plantId = plant.plantName + "_FERTILIZE",
                plantName = plant.plantName,
                type = "FERTILIZING",
                hour = time.first,
                minute = time.second,
                intervalMillis = fertilizingInterval
            )
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

    private fun getTimesForFrequency(freq: String): List<Pair<Int, Int>> {
        return when (freq) {
            "2 kali sehari" -> listOf(11 to 0, 17 to 0)
            "1 kali sehari" -> listOf(17 to 0)
            "2 hari sekali" -> listOf(17 to 0)
            "3 hari sekali" -> listOf(17 to 0)
            "7 hari sekali" -> listOf(17 to 0)
            "2 minggu sekali" -> listOf(17 to 0)
            "sebulan sekali" -> listOf(17 to 0)
            else -> listOf(17 to 0)
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
