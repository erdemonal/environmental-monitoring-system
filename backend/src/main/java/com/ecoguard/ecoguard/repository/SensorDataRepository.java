package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    Optional<SensorData> findTopByOrderByTimestampDesc();
    List<SensorData> findAllByTimestampBetween(LocalDateTime start, LocalDateTime end);
}