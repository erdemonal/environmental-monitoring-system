package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    Optional<Alert> findTopByOrderByTimestampDesc();
}