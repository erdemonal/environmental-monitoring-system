package com.ecoguard.ecoguard.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Represents a command sent to the ESP32 device.
 * <p>
 * Commands are queued by administrators and fetched by the device periodically.
 * Supported command types include SET_LED_COLOR, DISPLAY_MESSAGE, BLE_BROADCAST,
 * and REFRESH_CONFIG. The device acknowledges execution by updating the executed flag.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@Entity
@Table(name = "DEVICE_COMMANDS")
@Data
public class DeviceCommand {
    /**
     * Default constructor.
     */
    public DeviceCommand() {
    }

    /**
     * Unique identifier for the command.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "command_id")
    private Long id;

    /**
     * Device key identifying which ESP32 device should execute this command.
     * Currently supports "demo-device-key" for the single device setup.
     */
    @Column(name = "device_key", nullable = false, length = 100)
    private String deviceKey;

    /**
     * Type of command to execute.
     * Valid values: SET_LED_COLOR, DISPLAY_MESSAGE, BLE_BROADCAST, REFRESH_CONFIG.
     */
    @Column(name = "command_type", nullable = false, length = 50)
    private String commandType;

    /**
     * Optional parameters for the command.
     * Format depends on command type (e.g., "r,g,b" for SET_LED_COLOR).
     */
    @Column(name = "parameters", length = 500)
    private String parameters;

    /**
     * Flag indicating whether the device has executed this command.
     * Set to true when device acknowledges command execution.
     */
    @Column(name = "executed", nullable = false)
    private Boolean executed = false;

    /**
     * Timestamp when the command was created by the admin.
     * Automatically set to current time on persistence.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the device executed the command.
     * Set by the device when acknowledging command execution.
     */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    /**
     * JPA lifecycle callback to set creation timestamp before persisting.
     * Automatically sets {@code createdAt} to current time if not already set.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

