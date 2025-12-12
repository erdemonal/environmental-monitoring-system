package com.ecoguard.mobile.network.data

import com.google.gson.annotations.SerializedName

data class Threshold(
    @SerializedName("metricType")
    val metricType: String,
    @SerializedName("minValue")
    val minValue: Double?,
    @SerializedName("maxValue")
    val maxValue: Double?
)
