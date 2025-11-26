package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {
    List<DeviceCommand> findByDeviceKeyAndExecutedFalseOrderByCreatedAtAsc(String deviceKey);
    Optional<DeviceCommand> findByIdAndDeviceKey(Long id, String deviceKey);
    Optional<DeviceCommand> findTopByCommandTypeOrderByCreatedAtDesc(String commandType);
}

