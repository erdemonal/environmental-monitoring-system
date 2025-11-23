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

@RestController
@RequestMapping("/api/admin/sensor-data")
public class AdminSensorDataController {

    private final SensorDataRepository sensorDataRepository;

    public AdminSensorDataController(SensorDataRepository sensorDataRepository) {
        this.sensorDataRepository = sensorDataRepository;
    }

    @GetMapping
    public List<SensorData> getAll() {
        return sensorDataRepository.findAll();
    }

    @GetMapping("/latest")
    public ResponseEntity<SensorData> getLatest() {
        return sensorDataRepository.findTopByOrderByTimestampDesc()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/range")
    public List<SensorData> getRange(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return sensorDataRepository.findAllByTimestampBetween(start, end);
    }
}

