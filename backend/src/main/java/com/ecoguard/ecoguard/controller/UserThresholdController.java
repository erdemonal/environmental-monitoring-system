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

@RestController
@RequestMapping("/api/user/thresholds")
public class UserThresholdController {

    private final ThresholdRepository thresholdRepository;

    public UserThresholdController(ThresholdRepository thresholdRepository) {
        this.thresholdRepository = thresholdRepository;
    }

    @GetMapping
    public List<Threshold> getAllThresholds() {
        return thresholdRepository.findAll();
    }

    @GetMapping("/by-metric/{metricType}")
    public ResponseEntity<Threshold> getByMetric(@PathVariable MetricType metricType) {
        return thresholdRepository.findByMetricType(metricType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

