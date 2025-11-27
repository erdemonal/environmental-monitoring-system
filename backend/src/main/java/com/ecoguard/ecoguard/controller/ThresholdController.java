package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.config.AuthInterceptor;
import com.ecoguard.ecoguard.dto.ThresholdAuditResponse;
import com.ecoguard.ecoguard.dto.ThresholdPayload;
import com.ecoguard.ecoguard.entity.DeviceCommand;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import com.ecoguard.ecoguard.entity.ThresholdAudit;
import com.ecoguard.ecoguard.repository.DeviceCommandRepository;
import com.ecoguard.ecoguard.repository.ThresholdAuditRepository;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing environmental threshold configurations.
 * <p>
 * Provides endpoints for CRUD operations on thresholds, which define acceptable
 * ranges for each metric type (Temperature, Humidity, CO2, Light). When thresholds
 * are updated, the system automatically sends a REFRESH_CONFIG command to the ESP32
 * device and logs the change in the audit trail.
 * <p>
 * All endpoints require ADMIN role authentication.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/thresholds")
public class ThresholdController {

    private final ThresholdRepository thresholdRepository;
    private final ThresholdAuditRepository thresholdAuditRepository;
    private final DeviceCommandRepository deviceCommandRepository;

    /**
     * Constructs a new ThresholdController with required dependencies.
     *
     * @param thresholdRepository repository for threshold data access
     * @param thresholdAuditRepository repository for audit trail data access
     * @param deviceCommandRepository repository for device command management
     */
    public ThresholdController(ThresholdRepository thresholdRepository,
                               ThresholdAuditRepository thresholdAuditRepository,
                               DeviceCommandRepository deviceCommandRepository) {
        this.thresholdRepository = thresholdRepository;
        this.thresholdAuditRepository = thresholdAuditRepository;
        this.deviceCommandRepository = deviceCommandRepository;
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
     * Retrieves a threshold by its unique identifier.
     *
     * @param id the threshold ID to look up
     * @return ResponseEntity containing the threshold if found, or 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Threshold> getThresholdById(@PathVariable("id") Long id) {
        return thresholdRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
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


    /**
     * Updates an existing threshold configuration.
     * <p>
     * Updates the min/max values for a threshold and automatically:
     * <ul>
     *   <li>Logs the change in the audit trail with username and timestamp</li>
     *   <li>Sends a REFRESH_CONFIG command to the ESP32 device</li>
     * </ul>
     *
     * @param id the threshold ID to update
     * @param payload the new threshold values (minValue and/or maxValue)
     * @param request HTTP request containing the authentication session
     * @return ResponseEntity containing the updated threshold, or 404 Not Found
     */
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
            
            // Automatically send REFRESH_CONFIG command to device with threshold info
            DeviceCommand refreshCmd = new DeviceCommand();
            refreshCmd.setDeviceKey("demo-device-key");
            refreshCmd.setCommandType("REFRESH_CONFIG");
            // Include which metric was updated
            refreshCmd.setParameters("threshold_updated:" + saved.getMetricType().name());
            refreshCmd.setExecuted(false);
            deviceCommandRepository.save(refreshCmd);
            
            return ResponseEntity.ok(saved);
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves the most recent threshold change history.
     * <p>
     * Returns the last 10 threshold modifications, ordered by update time descending.
     * Used for displaying threshold change history in the admin panel.
     *
     * @return list of the 10 most recent threshold audit records
     */
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

    /**
     * Deletes a threshold configuration by ID.
     *
     * @param id the threshold ID to delete
     * @return ResponseEntity with 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        try {
            thresholdRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception ex) {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * Extracts the username from the authentication session.
     *
     * @param request HTTP request containing the session attribute
     * @return the username from the session, or "admin" as fallback
     */
    private String resolveUsername(HttpServletRequest request) {
        Object attr = request.getAttribute(AuthInterceptor.ATTR_SESSION);
        if (attr instanceof com.ecoguard.ecoguard.config.AuthTokenService.Session session) {
            return session.username();
        }
        return "admin";
    }
}