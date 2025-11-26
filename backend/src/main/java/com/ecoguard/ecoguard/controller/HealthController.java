package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.repository.SensorDataRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final SensorDataRepository sensorDataRepository;

    public HealthController(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

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


