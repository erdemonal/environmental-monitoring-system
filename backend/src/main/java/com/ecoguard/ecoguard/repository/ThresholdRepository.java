package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for threshold configuration persistence operations.
 * <p>
 * Provides methods for querying thresholds by metric type.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, Long> {
    /**
     * Finds the threshold configuration for a specific metric type.
     *
     * @param metricType the metric type to look up
     * @return Optional containing the threshold if found, or empty if not configured
     */
    Optional<Threshold> findByMetricType(MetricType metricType);
}