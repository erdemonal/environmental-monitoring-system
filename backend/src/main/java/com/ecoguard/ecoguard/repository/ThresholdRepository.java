package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Threshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ThresholdRepository extends JpaRepository<Threshold, Long> {
    Optional<Threshold> findByMetricType(MetricType metricType);
}