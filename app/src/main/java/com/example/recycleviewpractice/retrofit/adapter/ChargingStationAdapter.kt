package com.example.recycleviewpractice.retrofit.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.recycleviewpractice.databinding.ItemChargingStationBinding
import com.example.recycleviewpractice.retrofit.model.Station

class ChargingStationAdapter(
    private val chargingStations: List<Station>
) : RecyclerView.Adapter<ChargingStationAdapter.ChargingStationViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChargingStationViewHolder {
        // Inflate the layout using View Binding
        val binding = ItemChargingStationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChargingStationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChargingStationViewHolder, position: Int) {
        val chargingStation = chargingStations[position]
        // Bind the data to the ViewHolder
        holder.bind(chargingStation)
    }

    override fun getItemCount(): Int {
        return chargingStations.size
    }
    class ChargingStationViewHolder(private val binding: ItemChargingStationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(station: Station) {
            binding.tvChargingName.text = station.name
            binding.tvChargingLatitude.text = station.latitude.toString()
            binding.tvChargingLongitude.text = station.longitude.toString()
            binding.tvChargingType.text = station.type
        }
    }
}
