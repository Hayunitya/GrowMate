package com.example.growmate.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.growmate.databinding.ItemPlantBinding
import com.example.growmate.model.Plant

class PlantAdapter(private var plants: List<Plant>) : RecyclerView.Adapter<PlantAdapter.ViewHolder>() {
    inner class ViewHolder(val binding: ItemPlantBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val plant = plants[position]
        holder.binding.plantName.text = plant.name
        holder.binding.plantType.text = plant.type
        holder.binding.plantDate.text = plant.datePlanted
    }

    override fun getItemCount(): Int = plants.size

    fun updateData(newList: List<Plant>) {
        plants = newList
        notifyDataSetChanged()
    }
}