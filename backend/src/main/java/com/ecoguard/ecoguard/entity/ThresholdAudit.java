package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Audit trail entity for tracking threshold configuration changes.
 * <p>
 * Records all modifications to threshold values, including who made the change
 * and when. Used for compliance, debugging, and change history tracking.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Entity
@Table(name = "THRESHOLD_AUDIT")
@Data
public class ThresholdAudit {
    /**
     * Default constructor.
     */
    public ThresholdAudit() {
    }

    /**
     * Unique identifier for the audit record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID of the threshold that was modified.
     */
    @Column(name = "threshold_id")
    private Long thresholdId;

    /**
     * The metric type for which the threshold was changed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    /**
     * The new minimum value that was set.
     */
    @Column(name = "min_value", precision = 10, scale = 2)
    private BigDecimal minValue;

    /**
     * The new maximum value that was set.
     */
    @Column(name = "max_value", precision = 10, scale = 2)
    private BigDecimal maxValue;

    /**
     * Username of the admin who made the change.
     */
    @Column(name = "updated_by")
    private String updatedBy;

    /**
     * Timestamp when the threshold was updated.
     * Defaults to current time on creation.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
}

