package com.example.growmate.adapter

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.res.Resources
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.example.growmate.R
import com.example.growmate.databinding.ItemPlantBinding
import com.example.growmate.model.Plant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class PlantAdapter(
    private var plants: List<Plant>,
    private val onDelete: (Plant) -> Unit,
    private val onEdit: (Plant) -> Unit,
    private val onWatered: (Plant) -> Unit,
    private val onFertilized: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    inner class PlantViewHolder(val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        val context = holder.itemView.context
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now()

        // Nama & Tanggal
        holder.binding.tvPlantName.text = plant.plantName
        holder.binding.tvPlantDate.text = "Ditanam: ${plant.plantDate}"

        // Cek Status Penyiraman & Pemupukan
        val needsWater = plant.waterStatus == "Belum Disiram"
        val needsFertilize = plant.fertilizerStatus == "Belum Dipupuk"

        // Set Text Status Kombinasi
        val statusText = when {
            needsWater && needsFertilize -> "Status: Perlu disiram & perlu dipupuk"
            needsWater -> "Status: Perlu disiram"
            needsFertilize -> "Status: Perlu dipupuk"
            else -> "Status: Terpenuhi"
        }
        holder.binding.tvWaterStatus.text = statusText

        // Tombol Penyiraman
        holder.binding.btnWaterNow.apply {
            isEnabled = needsWater
            text = if (needsWater) "Siram Sekarang" else "Sudah Disiram"
            setOnClickListener { onWatered(plant) }
        }

        // Tombol Pemupukan
        holder.binding.btnFertilizerNow.apply {
            isEnabled = needsFertilize
            text = if (needsFertilize) "Pupuk Sekarang" else "Sudah Dipupuk"
            setOnClickListener { onFertilized(plant) }
        }


        // Icon Pertumbuhan
        val iconRes = try {
            val plantedDate = LocalDate.parse(plant.plantDate, formatter)
            val ageDays = ChronoUnit.DAYS.between(plantedDate, today)
            when {
                ageDays < 3 -> R.drawable.ic_seed
                ageDays < 7 -> R.drawable.ic_sprout
                ageDays < 14 -> R.drawable.ic_plant
                else -> R.drawable.ic_tree
            }
        } catch (e: Exception) {
            R.drawable.ic_seed
        }
        holder.binding.imgGrowthStage.setImageResource(iconRes)

        holder.binding.tvWaterStreakCount.text = plant.waterStreak.toString()

        holder.binding.tvFertilizerStreakCount.text = plant.fertilizerStreak.toString()

        animateStreak(holder.binding.tvWaterStreakCount)
        animateStreak(holder.binding.tvFertilizerStreakCount)

        // Edit Click
        holder.binding.cardView.setOnClickListener { onEdit(plant) }
    }

    private fun animateStreak(view: View) {
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.2f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.2f)
        ).apply {
            duration = 150
            repeatMode = ValueAnimator.REVERSE
            repeatCount = 1
        }
        scaleUp.start()
    }

    override fun getItemCount(): Int = plants.size

    fun updateData(newList: List<Plant>) {
        plants = newList
        notifyDataSetChanged()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }
}
