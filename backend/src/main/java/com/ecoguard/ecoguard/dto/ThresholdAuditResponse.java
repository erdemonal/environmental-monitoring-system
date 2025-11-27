package com.ecoguard.ecoguard.dto;

import com.ecoguard.ecoguard.entity.MetricType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for threshold audit trail records.
 * <p>
 * Represents a historical record of threshold configuration changes.
 * Used by the admin panel to display threshold change history.
 *
 * @param id unique identifier for the audit record
 * @param thresholdId ID of the threshold that was modified
 * @param metricType the metric type that was changed
 * @param minValue the new minimum value that was set
 * @param maxValue the new maximum value that was set
 * @param updatedBy username of the admin who made the change
 * @param updatedAt timestamp when the change was made
 *
 * @author EcoGuard 
 * @since 1.0
 */
public record ThresholdAuditResponse(
        Long id,
        Long thresholdId,
        MetricType metricType,
        BigDecimal minValue,
        BigDecimal maxValue,
        String updatedBy,
        LocalDateTime updatedAt
) {}


