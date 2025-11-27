package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.ThresholdAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for threshold audit trail persistence operations.
 * <p>
 * Provides methods for querying threshold change history.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Repository
public interface ThresholdAuditRepository extends JpaRepository<ThresholdAudit, Long> {
    /**
     * Finds the 10 most recent threshold audit records.
     *
     * @return list of the 10 most recent audit records, ordered by update time descending
     */
    List<ThresholdAudit> findTop10ByOrderByUpdatedAtDesc();
}

