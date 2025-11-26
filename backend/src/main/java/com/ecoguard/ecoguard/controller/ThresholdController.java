package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.config.AuthInterceptor;
import com.ecoguard.ecoguard.dto.ThresholdAuditResponse;
import com.ecoguard.ecoguard.dto.ThresholdPayload;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import com.ecoguard.ecoguard.entity.ThresholdAudit;
import com.ecoguard.ecoguard.repository.ThresholdAuditRepository;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/thresholds")
public class ThresholdController {

    private final ThresholdRepository thresholdRepository;
    private final ThresholdAuditRepository thresholdAuditRepository;

    public ThresholdController(ThresholdRepository thresholdRepository,
                               ThresholdAuditRepository thresholdAuditRepository) {
        this.thresholdRepository = thresholdRepository;
        this.thresholdAuditRepository = thresholdAuditRepository;
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
    public ResponseEntity<?> update(@PathVariable("id") Long id,
                                    @RequestBody ThresholdPayload payload,
                                    HttpServletRequest request) {
        return thresholdRepository.findById(id).map(existing -> {
            if (payload.getMinValue() != null) {
                existing.setMinValue(payload.getMinValue());
            }
            if (payload.getMaxValue() != null) {
                existing.setMaxValue(payload.getMaxValue());
            }
            Threshold saved = thresholdRepository.save(existing);
            ThresholdAudit audit = new ThresholdAudit();
            audit.setThresholdId(saved.getId());
            audit.setMetricType(saved.getMetricType());
            audit.setMinValue(saved.getMinValue());
            audit.setMaxValue(saved.getMaxValue());
            audit.setUpdatedAt(LocalDateTime.now());
            String updatedBy = resolveUsername(request);
            audit.setUpdatedBy(updatedBy);
            thresholdAuditRepository.save(audit);
            return ResponseEntity.ok(saved);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/audit")
    public List<ThresholdAuditResponse> recentAudits() {
        return thresholdAuditRepository.findTop10ByOrderByUpdatedAtDesc().stream()
                .map(a -> new ThresholdAuditResponse(
                        a.getId(),
                        a.getThresholdId(),
                        a.getMetricType(),
                        a.getMinValue(),
                        a.getMaxValue(),
                        a.getUpdatedBy(),
                        a.getUpdatedAt()
                ))
                .collect(Collectors.toList());
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

    private String resolveUsername(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.ATTR_SESSION);
        if (attr instanceof com.ecoguard.ecoguard.config.AuthTokenService.Session session) {
            return session.username();
        }
        return "admin";
    }
}