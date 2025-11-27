package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.repository.SensorDataRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for system health monitoring.
 * <p>
 * Provides a health check endpoint that reports application status and database
 * connectivity. Used by monitoring tools and displayed in the admin panel's
 * System Health card.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final SensorDataRepository sensorDataRepository;

    /**
     * Constructs a new HealthController with required dependencies.
     *
     * @param sensorDataRepository repository used to test database connectivity
     */
    public HealthController(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

    /**
     * Performs a health check on the application and database.
     * <p>
     * Returns the application status (always "UP" if endpoint is reachable),
     * current server time, and database status with sensor data row count.
     * If database access fails, the db status is set to "DOWN" with error details.
     *
     * @return ResponseEntity containing health status information
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new HashMap<>();
        body.put("status", "UP");
        body.put("time", Instant.now().toString());

        try {
            long count = sensorDataRepository.count();
            Map<String, Object> db = new HashMap<>();
            db.put("status", "UP");
            db.put("sensorDataCount", count);
            body.put("db", db);
        } catch (Exception e) {
            Map<String, Object> db = new HashMap<>();
            db.put("status", "DOWN");
            db.put("error", e.getClass().getSimpleName());
            body.put("db", db);
        }

        return ResponseEntity.ok(body);
    }
}


