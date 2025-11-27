package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.entity.SensorData;
import com.ecoguard.ecoguard.repository.SensorDataRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for admin access to sensor data.
 * <p>
 * Provides the same endpoints as SensorDataController but requires ADMIN role.
 * Allows administrators to view all sensor readings and query by time range.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/sensor-data")
public class AdminSensorDataController {

    private final SensorDataRepository sensorDataRepository;

    /**
     * Constructs a new AdminSensorDataController with required dependencies.
     *
     * @param sensorDataRepository repository for sensor data access
     */
    public AdminSensorDataController(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

    /**
     * Retrieves all sensor readings from the database.
     *
     * @return list of all sensor data records
     */
    @GetMapping
    public List<SensorData> getAll() {
        return sensorDataRepository.findAll();
    }

    /**
     * Retrieves the most recent sensor reading.
     *
     * @return ResponseEntity containing the latest sensor data, or 404 Not Found if no data exists
     */
    @GetMapping("/latest")
    public ResponseEntity<SensorData> getLatest() {
        return sensorDataRepository.findTopByOrderByTimestampDesc()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves sensor readings within a specified time range.
     *
     * @param start the start timestamp (ISO 8601 format)
     * @param end the end timestamp (ISO 8601 format)
     * @return list of sensor data records within the specified time range
     */
    @GetMapping("/range")
    public List<SensorData> getRange(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return sensorDataRepository.findAllByTimestampBetween(start, end);
    }
}

