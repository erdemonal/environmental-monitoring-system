package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.dto.ThresholdPayload;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/thresholds")
public class ThresholdController {

    private final ThresholdRepository thresholdRepository;

    public ThresholdController(ThresholdRepository thresholdRepository) {
        this.thresholdRepository = thresholdRepository;
    }

    @GetMapping
    public List<Threshold> getAllThresholds() {
        return thresholdRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Threshold> getThresholdById(@PathVariable("id") Long id) {
        return thresholdRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/by-metric/{metricType}")
    public ResponseEntity<Threshold> getByMetric(@PathVariable MetricType metricType) {
        return thresholdRepository.findByMetricType(metricType)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody ThresholdPayload payload) {
        return thresholdRepository.findById(id).map(existing -> {
            if (payload.getMinValue() != null) {
                existing.setMinValue(payload.getMinValue());
            }
            if (payload.getMaxValue() != null) {
                existing.setMaxValue(payload.getMaxValue());
            }
            if (payload.getDisabled() != null) {
                existing.setDisabled(payload.getDisabled());
            }
            return ResponseEntity.ok(thresholdRepository.save(existing));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            thresholdRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.noContent().build();
        }
    }
}