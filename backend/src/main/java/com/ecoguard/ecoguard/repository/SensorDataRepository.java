package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for sensor data persistence operations.
 * <p>
 * Provides methods for querying sensor readings by timestamp and retrieving
 * the most recent reading.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    /**
     * Finds the most recent sensor reading.
     *
     * @return Optional containing the latest sensor data, or empty if no data exists
     */
    Optional<SensorData> findTopByOrderByTimestampDesc();
    
    /**
     * Finds all sensor readings within a specified time range.
     *
     * @param start the start timestamp (inclusive)
     * @param end the end timestamp (inclusive)
     * @return list of sensor data records within the time range
     */
    List<SensorData> findAllByTimestampBetween(LocalDateTime start, LocalDateTime end);
}