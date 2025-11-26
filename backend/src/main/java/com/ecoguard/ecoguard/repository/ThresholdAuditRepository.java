package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.ThresholdAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThresholdAuditRepository extends JpaRepository<ThresholdAudit, Long> {
    List<ThresholdAudit> findTop10ByOrderByUpdatedAtDesc();
}

