package com.ecoguard.mobile.network.data

import com.google.gson.annotations.SerializedName

data class DeviceTokenRequest(
    @SerializedName("token")
    val token: String
)
