package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents device configuration for ESP32 network settings.
 * <p>
 * Stores WiFi credentials and backend connection information for remote device configuration.
 * This entity is currently deprecated in favor of hardcoded configuration in the embedded code.
 *
 * @author EcoGuard
 * @since 1.0
 */
@Entity
@Table(name = "DEVICE_CONFIG")
@Data
public class DeviceConfig {
    /**
     * Default constructor.
     */
    public DeviceConfig() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long id;

    @Column(name = "device_key", nullable = false, unique = true, length = 100)
    private String deviceKey;

    @Column(name = "wifi_ssid", length = 100)
    private String wifiSsid;

    @Column(name = "wifi_password", length = 100)
    private String wifiPassword;

    @Column(name = "backend_host", length = 100)
    private String backendHost;

    @Column(name = "backend_port")
    private Integer backendPort;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback to update timestamp before persisting or updating.
     * Automatically sets {@code updatedAt} to current time.
     */
    @PrePersist
    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }
}

