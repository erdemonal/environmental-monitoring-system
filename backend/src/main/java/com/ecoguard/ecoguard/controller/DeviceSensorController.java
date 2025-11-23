package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.dto.SensorDataPayload;
import com.ecoguard.ecoguard.entity.Alert;
import com.ecoguard.ecoguard.entity.AlertType;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.SensorData;
import com.ecoguard.ecoguard.repository.AlertRepository;
import com.ecoguard.ecoguard.repository.SensorDataRepository;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceSensorController {

    private final SensorDataRepository sensorDataRepository;
    private final ThresholdRepository thresholdRepository;
    private final AlertRepository alertRepository;

    public DeviceSensorController(SensorDataRepository sensorDataRepository,
                                  ThresholdRepository thresholdRepository,
                                  AlertRepository alertRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.thresholdRepository = thresholdRepository;
        this.alertRepository = alertRepository;
    }

    @PostMapping("/sensor-data")
    public ResponseEntity<?> ingest(@RequestBody SensorDataPayload payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Payload required"));
        }
        boolean hasValue = payload.getTemperature() != null
                || payload.getHumidity() != null
                || payload.getCo2Level() != null
                || payload.getLightLevel() != null;
        if (!hasValue) {
            return ResponseEntity.badRequest().body(Map.of("message", "At least one metric (temperature, humidity, co2Level, lightLevel) required"));
        }

        SensorData data = new SensorData();
        data.setTemperature(payload.getTemperature());
        data.setHumidity(payload.getHumidity());
        data.setCo2Level(payload.getCo2Level());
        data.setLightLevel(payload.getLightLevel());
        if (payload.getTimestamp() != null) {
            data.setTimestamp(payload.getTimestamp());
        } else {
            data.setTimestamp(LocalDateTime.now());
        }
        SensorData saved = sensorDataRepository.save(data);

        List<Alert> alerts = new ArrayList<>();
        evaluateMetric(MetricType.TEMP, toBigDecimal(payload.getTemperature()), alerts);
        evaluateMetric(MetricType.HUMIDITY, toBigDecimal(payload.getHumidity()), alerts);
        evaluateMetric(MetricType.CO2, toBigDecimal(payload.getCo2Level()), alerts);
        evaluateMetric(MetricType.LIGHT, toBigDecimal(payload.getLightLevel()), alerts);

        return ResponseEntity.ok(Map.of(
                "sensorDataId", saved.getId(),
                "alertsCreated", alerts.stream().map(Alert::getId).toList()
        ));
    }

    private void evaluateMetric(MetricType metric, BigDecimal value, List<Alert> alerts) {
        if (value == null) {
            return;
        }
        thresholdRepository.findByMetricType(metric).ifPresent(threshold -> {
            // Skip if threshold is disabled
            if (Boolean.TRUE.equals(threshold.getDisabled())) {
                return;
            }
            // Check if value is outside threshold range
            if (value.compareTo(threshold.getMinValue()) < 0 || value.compareTo(threshold.getMaxValue()) > 0) {
                Alert alert = new Alert();
                alert.setAlertType(AlertType.THRESHOLD);
                alert.setMetricType(metric.name());
                alert.setValue(value);
                alert.setTimestamp(LocalDateTime.now());
                Alert savedAlert = alertRepository.save(alert);
                alerts.add(savedAlert);
            }
        });
    }

    private BigDecimal toBigDecimal(BigDecimal value) {
        return value;
    }

    private BigDecimal toBigDecimal(Integer value) {
        return value == null ? null : new BigDecimal(value);
    }
}

