package com.ecoguard.ecoguard.repository;

import com.ecoguard.ecoguard.entity.DeviceConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository interface for device configuration persistence operations.
 * <p>
 * Provides methods for querying device configurations by device key.
 * Currently deprecated in favor of hardcoded configuration.
 *
 * @author EcoGuard
 * @since 1.0
 */
public interface DeviceConfigRepository extends JpaRepository<DeviceConfig, Long> {
    /**
     * Finds device configuration by device key.
     *
     * @param deviceKey the device key to search for
     * @return Optional containing the device config if found, or empty
     */
    Optional<DeviceConfig> findByDeviceKey(String deviceKey);
}

