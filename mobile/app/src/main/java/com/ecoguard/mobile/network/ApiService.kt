package com.ecoguard.mobile.network

import com.ecoguard.mobile.network.data.Alert
import com.ecoguard.mobile.network.data.DeviceTokenRequest
import com.ecoguard.mobile.network.data.LoginRequest
import com.ecoguard.mobile.network.data.LoginResponse
import com.ecoguard.mobile.network.data.SensorData
import com.ecoguard.mobile.network.data.Threshold
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/login")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse

    @GET("api/user/sensor-data")
    suspend fun getSensorData(): List<SensorData>

    @GET("api/user/sensor-data/latest")
    suspend fun getLatestSensorData(): SensorData

    @GET("api/user/alerts")
    suspend fun getAlerts(): List<Alert>

    @PUT("api/user/alerts/{id}/acknowledge")
    suspend fun acknowledgeAlert(@Path("id") id: Long)

    @GET("api/user/thresholds")
    suspend fun getThresholds(): List<Threshold>

    @PUT("api/auth/device-token")
    suspend fun updateDeviceToken(@Body deviceTokenRequest: DeviceTokenRequest)
}
