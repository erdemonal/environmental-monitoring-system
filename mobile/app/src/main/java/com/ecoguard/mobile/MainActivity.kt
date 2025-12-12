package com.ecoguard.mobile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import com.ecoguard.mobile.network.ApiService
import com.ecoguard.mobile.network.AuthInterceptor
import com.ecoguard.mobile.network.data.Alert
import com.ecoguard.mobile.network.data.DeviceTokenRequest
import com.ecoguard.mobile.network.data.SensorData
import com.ecoguard.mobile.network.data.Threshold
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var sensorDataAdapter: SensorDataAdapter
    private lateinit var apiService: ApiService
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is required to receive alerts.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.sensor_data_recyclerview)
        progressBar = findViewById(R.id.progress_bar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        recyclerView.layoutManager = LinearLayoutManager(this)
        sensorDataAdapter = SensorDataAdapter(mutableListOf<SensorData>())
        recyclerView.adapter = sensorDataAdapter

        swipeRefresh.setOnRefreshListener {
            fetchAndDisplaySensorData(isPullToRefresh = true)
        }

        requestNotificationPermission()

        val token = intent.getStringExtra("AUTH_TOKEN")
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Authentication token not found. Please login again.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupApiService(token)
        fetchAndDisplaySensorData()
        sendDeviceTokenToBackend()
        
        val fabAlerts = findViewById<FloatingActionButton>(R.id.fab_alerts)
        fabAlerts.setOnClickListener {
            showAlerts()
        }
        
        val fabLatest = findViewById<FloatingActionButton>(R.id.fab_latest)
        fabLatest.setOnClickListener {
            getLatestReading()
        }
    }

    private fun setupApiService(token: String) {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(token))
            .build()

        val retrofit = Retrofit.Builder()
            // Emulator uses 10.0.2.2 to reach host backend
            .baseUrl("http://10.0.2.2:8080/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun fetchAndDisplaySensorData(isPullToRefresh: Boolean = false) {
        lifecycleScope.launch {
            if (isPullToRefresh) {
                swipeRefresh.isRefreshing = true
            } else {
                progressBar.visibility = View.VISIBLE
            }
            try {
                val sensorDataList = apiService.getSensorData()
                    .sortedByDescending { it.timestamp }
                sensorDataAdapter.updateData(sensorDataList)
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun sendDeviceTokenToBackend() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val deviceToken = task.result

            // Log and send to backend
            Log.d("FCM", "FCM Token: $deviceToken")
            lifecycleScope.launch {
                try {
                    apiService.updateDeviceToken(DeviceTokenRequest(deviceToken))
                    Log.d("FCM", "Device token sent to backend successfully.")
                } catch (e: Exception) {
                    Log.e("FCM", "Failed to send device token to backend", e)
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun showAlerts() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val alerts = apiService.getAlerts()
                    .sortedByDescending { it.timestamp }
                    .take(10) // Show last 10 alerts
                
                if (alerts.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No alerts found", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val alertMessages = alerts.joinToString("\n\n") { alert ->
                    "${alert.metricType}: ${alert.value}\nTime: ${alert.timestamp}"
                }
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Recent Alerts (${alerts.size})")
                    .setMessage(alertMessages)
                    .setPositiveButton("OK") { _, _ ->
                        // Acknowledge all alerts
                        acknowledgeAllAlerts(alerts.map { it.id })
                    }
                    .setNeutralButton("View Thresholds") { _, _ ->
                        showThresholds()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load alerts: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showThresholds() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val thresholds = apiService.getThresholds()
                
                if (thresholds.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No thresholds configured", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val thresholdMessages = thresholds.joinToString("\n\n") { threshold ->
                    "${threshold.metricType}:\n  Min: ${threshold.minValue ?: "N/A"}\n  Max: ${threshold.maxValue ?: "N/A"}"
                }
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Thresholds")
                    .setMessage(thresholdMessages)
                    .setPositiveButton("OK", null)
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to load thresholds: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun acknowledgeAllAlerts(alertIds: List<Long>) {
        lifecycleScope.launch {
            try {
                alertIds.forEach { id ->
                    apiService.acknowledgeAlert(id)
                }
                Toast.makeText(this@MainActivity, "Alerts acknowledged", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to acknowledge alerts: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun getLatestReading() {
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            try {
                val latest = apiService.getLatestSensorData()
                
                val message = """
                    Latest Sensor Reading:
                    
                    Temperature: ${latest.temperature}°C
                    Humidity: ${latest.humidity}%
                    CO₂: ${latest.co2} ppm
                    Light: ${latest.lightLevel} lux
                    
                    Time: ${latest.timestamp}
                """.trimIndent()
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Latest Reading")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Refresh All") { _, _ ->
                        fetchAndDisplaySensorData()
                    }
                    .show()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to get latest reading: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
}
