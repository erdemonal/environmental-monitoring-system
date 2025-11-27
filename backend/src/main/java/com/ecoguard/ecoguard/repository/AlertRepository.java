package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for alert persistence operations.
 * <p>
 * Provides methods for querying alerts by timestamp.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    /**
     * Finds the most recent alert.
     *
     * @return Optional containing the latest alert, or empty if no alerts exist
     */
    Optional<Alert> findTopByOrderByTimestampDesc();
}