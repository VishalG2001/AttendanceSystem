package com.example.recycleviewpractice.bluetoothConnection

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BluetoothDeviceAdapter(private val devices: List<BluetoothDevice>, private val onClick: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
        holder.itemView.setOnClickListener { onClick(device) }
    }

    override fun getItemCount(): Int = devices.size

    class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(android.R.id.text1)
        fun bind(device: BluetoothDevice) {
            deviceName.text = device.name
        }
    }
}
