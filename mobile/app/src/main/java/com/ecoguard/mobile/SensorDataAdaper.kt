package com.ecoguard.mobile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ecoguard.mobile.network.data.SensorData

class SensorDataAdapter(private val sensorDataList: MutableList<SensorData>) : RecyclerView.Adapter<SensorDataAdapter.SensorDataViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorDataViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_sensor_data, parent, false)
        return SensorDataViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: SensorDataViewHolder, position: Int) {
        val currentItem = sensorDataList[position]
        holder.temperatureValue.text = "${currentItem.temperature} C"
        holder.humidityValue.text = "${currentItem.humidity} %"
        holder.co2Value.text = "${currentItem.co2} ppm"
        holder.lightValue.text = "${currentItem.lightLevel} lux"
        holder.timestampValue.text = currentItem.timestamp
    }

    override fun getItemCount() = sensorDataList.size

    fun updateData(newData: List<SensorData>) {
        sensorDataList.clear()
        sensorDataList.addAll(newData)
        notifyDataSetChanged()
    }

    class SensorDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val temperatureValue: TextView = itemView.findViewById(R.id.temperature_value)
        val humidityValue: TextView = itemView.findViewById(R.id.humidity_value)
        val co2Value: TextView = itemView.findViewById(R.id.co2_value)
        val lightValue: TextView = itemView.findViewById(R.id.light_value)
        val timestampValue: TextView = itemView.findViewById(R.id.timestamp_value)
    }
}
