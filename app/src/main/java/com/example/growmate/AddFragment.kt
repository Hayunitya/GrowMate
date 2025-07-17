package com.example.growmate

import android.R
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.*
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.NumberPicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.growmate.databinding.FragmentAddBinding
import com.example.growmate.model.Plant
import com.example.growmate.viewmodel.PlantViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private lateinit var plantViewModel: PlantViewModel
    private val auth = FirebaseAuth.getInstance()

    private var selectedTime: Calendar = Calendar.getInstance()


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        view?.let { super.onViewCreated(it, savedInstanceState) }

        setupFrequencySpinner()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        plantViewModel = ViewModelProvider(this)[PlantViewModel::class.java]

        binding.etPlantDate.setOnClickListener { showDatePicker(binding.etPlantDate) }
        binding.etNextWateringDate.setOnClickListener { showDatePicker(binding.etNextWateringDate) }


        binding.btnSelectTime.setOnClickListener {
            showHourOnlyTimePicker()
        }
        binding.btnSave.setOnClickListener {
            val selectedFrequency = binding.spinnerFrequency.selectedItem.toString()

            when (selectedFrequency) {
                "1 hari sekali" -> {
                    setRepeatingAlarm(selectedTime, 1)
                }
                "Sehari 2 kali" -> {
                    setRepeatingAlarm(selectedTime, 0) // Pertama
                    val secondTime = (selectedTime.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 12) } // Misal 12 jam setelahnya
                    setRepeatingAlarm(secondTime, 0)
                }
                "Sehari 3 kali" -> {
                    setRepeatingAlarm(selectedTime, 0)
                    val secondTime = (selectedTime.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 8) }
                    val thirdTime = (selectedTime.clone() as Calendar).apply { add(Calendar.HOUR_OF_DAY, 16) }
                    setRepeatingAlarm(secondTime, 0)
                    setRepeatingAlarm(thirdTime, 0)
                }
                "2 hari sekali" -> {
                    setRepeatingAlarm(selectedTime, 2)
                }
                "3 hari sekali" -> {
                    setRepeatingAlarm(selectedTime, 3)
                }
                "1 kali seminggu" -> {
                    setRepeatingAlarm(selectedTime, 7)
                }
                "2 kali seminggu" -> {
                    setRepeatingAlarm(selectedTime, 0) // Pertama
                    val secondTime = (selectedTime.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 3) } // 3 hari kemudian
                    setRepeatingAlarm(secondTime, 0)
                }
            }
        }
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

    private fun setupFrequencySpinner() {
        val frequencies = listOf(
            "1 kali seminggu",
            "2 kali seminggu",
            "3 hari sekali",
            "2 hari sekali",
            "1 hari sekali",
            "Sehari 2 kali",
            "Sehari 3 kali"
        )

        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, frequencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFrequency.adapter = adapter

        // Optional: Ambil value yang dipilih
        binding.spinnerFrequency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = frequencies[position]
                Toast.makeText(requireContext(), "Kamu pilih: $selected", Toast.LENGTH_SHORT).show()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }
    private fun showHourOnlyTimePicker() {
        val timePicker = TimePickerDialog(
            requireContext(),
            { _, hourOfDay, _ ->
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, 0)
                selectedTime.set(Calendar.SECOND, 0)

                val timeFormat = SimpleDateFormat("hh:00 a", Locale.getDefault())
                binding.tvSelectedTime.text = timeFormat.format(selectedTime.time)
            },
            selectedTime.get(Calendar.HOUR_OF_DAY),
            0, // Minutes always start at 0
            false // 12-hour format with AM/PM
        )

        // Setelah show, force minutes ke 0 dan disable input manual (optional)
        timePicker.setOnShowListener {
            val minutePicker = timePicker.findViewById<NumberPicker>(
                Resources.getSystem().getIdentifier("minute", "id", "android")
            )
            minutePicker?.minValue = 0
            minutePicker?.maxValue = 0
        }

        timePicker.show()
    }

    private fun setRepeatingAlarm(time: Calendar, intervalInDays: Int) {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(requireContext(), WateringReceiver::class.java)

        // Biar tiap alarm unik, kita random ID
        val requestCode = (0..9999).random()
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = if (time.timeInMillis < System.currentTimeMillis()) {
            time.timeInMillis + AlarmManager.INTERVAL_DAY * (if (intervalInDays == 0) 1 else intervalInDays)
        } else {
            time.timeInMillis
        }

        val intervalMillis = if (intervalInDays == 0) {
            AlarmManager.INTERVAL_DAY
        } else {
            intervalInDays * AlarmManager.INTERVAL_DAY
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            intervalMillis,
            pendingIntent
        )
    }


}
