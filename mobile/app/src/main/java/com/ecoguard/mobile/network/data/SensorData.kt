package com.ecoguard.mobile.network.data

import com.google.gson.annotations.SerializedName

data class SensorData(
    val id: Long,
    val temperature: Double,
    val humidity: Double,
    @SerializedName("co2Level")
    val co2: Int,
    val lightLevel: Int,
    val timestamp: String
)
