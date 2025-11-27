package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for user access to threshold configurations.
 * <p>
 * Provides read-only endpoints for retrieving threshold values.
 * All endpoints require authentication (USER or ADMIN role).
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/user/thresholds")
public class UserThresholdController {

    private final ThresholdRepository thresholdRepository;

    /**
     * Constructs a new UserThresholdController with required dependencies.
     *
     * @param thresholdRepository repository for threshold data access
     */
    public UserThresholdController(ThresholdRepository thresholdRepository) {
        this.thresholdRepository = thresholdRepository;
    }

    /**
     * Retrieves all threshold configurations.
     *
     * @return list of all thresholds in the system
     */
    @GetMapping
    public List<Threshold> getAllThresholds() {
        return thresholdRepository.findAll();
    }

    /**
     * Retrieves the threshold configuration for a specific metric type.
     *
     * @param metricType the metric type (TEMP, HUMIDITY, CO2, or LIGHT)
     * @return ResponseEntity containing the threshold if found, or 404 Not Found
     */
    @GetMapping("/by-metric/{metricType}")
    public ResponseEntity<Threshold> getByMetric(@PathVariable MetricType metricType) {
        return thresholdRepository.findByMetricType(metricType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

