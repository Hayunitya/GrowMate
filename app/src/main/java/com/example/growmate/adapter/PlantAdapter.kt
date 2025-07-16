package com.example.growmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.growmate.databinding.ItemPlantBinding
import com.example.growmate.model.Plant

class PlantAdapter(
    private var plants: List<Plant>,
    private val onDelete: (Plant) -> Unit,
    private val onEdit: (Plant) -> Unit
) : RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    inner class PlantViewHolder(val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(plant: Plant) {
            binding.tvPlantName.text = plant.plantName
            binding.tvPlantType.text = plant.plantType
            binding.tvPlantDate.text = "Ditambahkan: ${plant.plantDate}"

            binding.btnDelete.setOnClickListener { onDelete(plant) }
            binding.btnEdit.setOnClickListener { onEdit(plant) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PlantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        holder.bind(plants[position])
    }

    override fun getItemCount(): Int = plants.size

    fun updateData(newList: List<Plant>) {
        plants = newList
        notifyDataSetChanged()
    }
}
