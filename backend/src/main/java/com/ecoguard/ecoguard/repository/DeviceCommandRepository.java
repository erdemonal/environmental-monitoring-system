package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for device command persistence operations.
 * <p>
 * Provides methods for querying commands by device key, execution status, and command type.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Repository
public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {
    /**
     * Finds pending (unexecuted) commands for a device, ordered by creation time.
     *
     * @param deviceKey the device key to filter by
     * @return list of pending commands, oldest first
     */
    List<DeviceCommand> findByDeviceKeyAndExecutedFalseOrderByCreatedAtAsc(String deviceKey);
    
    /**
     * Finds a command by ID and device key.
     *
     * @param id the command ID
     * @param deviceKey the device key
     * @return Optional containing the command if found, or empty
     */
    Optional<DeviceCommand> findByIdAndDeviceKey(Long id, String deviceKey);
    
    /**
     * Finds the most recent command of a specific type.
     *
     * @param commandType the command type to filter by
     * @return Optional containing the most recent command of the type, or empty
     */
    Optional<DeviceCommand> findTopByCommandTypeOrderByCreatedAtDesc(String commandType);
    
    /**
     * Finds the 10 most recent commands for a device.
     *
     * @param deviceKey the device key to filter by
     * @return list of the 10 most recent commands, newest first
     */
    List<DeviceCommand> findTop10ByDeviceKeyOrderByCreatedAtDesc(String deviceKey);
}

