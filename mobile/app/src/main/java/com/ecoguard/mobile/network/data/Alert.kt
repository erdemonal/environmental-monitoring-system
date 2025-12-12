package com.ecoguard.mobile.network.data

import com.google.gson.annotations.SerializedName

data class Alert(
    val id: Long,
    @SerializedName("alertType")
    val alertType: String,
    @SerializedName("metricType")
    val metricType: String,
    val value: Double,
    val timestamp: String
)

