package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.dto.DeviceStatusResponse;
import com.ecoguard.ecoguard.entity.Alert;
import com.ecoguard.ecoguard.entity.DeviceCommand;
import com.ecoguard.ecoguard.entity.SensorData;
import com.ecoguard.ecoguard.repository.AlertRepository;
import com.ecoguard.ecoguard.repository.DeviceCommandRepository;
import com.ecoguard.ecoguard.repository.SensorDataRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * REST controller for device status monitoring (admin only).
 * <p>
 * Provides comprehensive device status information including latest sensor readings,
 * online/offline status, last alert, and last admin message. Used by the admin panel's
 * Device Status card.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/device/status")
public class AdminDeviceStatusController {

    /**
     * Time threshold in seconds for determining if device is online.
     * Device is considered online if last data was received within this window.
     */
    private static final long ONLINE_THRESHOLD_SECONDS = 20;

    private final SensorDataRepository sensorDataRepository;
    private final AlertRepository alertRepository;
    private final DeviceCommandRepository deviceCommandRepository;

    /**
     * Constructs a new AdminDeviceStatusController with required dependencies.
     *
     * @param sensorDataRepository repository for retrieving latest sensor data
     * @param alertRepository repository for retrieving latest alert
     * @param deviceCommandRepository repository for retrieving last admin message
     */
    public AdminDeviceStatusController(SensorDataRepository sensorDataRepository,
                                       AlertRepository alertRepository,
                                       DeviceCommandRepository deviceCommandRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.alertRepository = alertRepository;
        this.deviceCommandRepository = deviceCommandRepository;
    }

    /**
     * Retrieves comprehensive device status information.
     * <p>
     * Aggregates data from multiple sources to provide a complete device status:
     * latest sensor readings, online/offline status, last alert, and last admin message.
     * Device is considered online if last data was received within 20 seconds.
     *
     * @return ResponseEntity containing device status information
     */
    @GetMapping
    public ResponseEntity<DeviceStatusResponse> getStatus() {
        Optional<SensorData> latestData = sensorDataRepository.findTopByOrderByTimestampDesc();
        Optional<Alert> latestAlert = alertRepository.findTopByOrderByTimestampDesc();
        Optional<DeviceCommand> latestMessage = deviceCommandRepository.findTopByCommandTypeOrderByCreatedAtDesc("DISPLAY_MESSAGE");

        SensorData data = latestData.orElse(null);
        boolean online = false;
        Long secondsSinceLastSeen = null;
        if (data != null && data.getTimestamp() != null) {
            LocalDateTime now = LocalDateTime.now();
            secondsSinceLastSeen = Math.max(0, Duration.between(data.getTimestamp(), now).getSeconds());
            online = secondsSinceLastSeen <= ONLINE_THRESHOLD_SECONDS;
        }
        return ResponseEntity.ok(new DeviceStatusResponse(
                data != null ? data.getTimestamp() : null,
                data != null ? data.getTemperature() : null,
                data != null ? data.getHumidity() : null,
                data != null ? data.getCo2Level() : null,
                data != null ? data.getLightLevel() : null,
                latestAlert.map(Alert::getTimestamp).orElse(null),
                latestAlert.map(Alert::getAlertType).map(Enum::name).orElse(null),
                latestMessage.map(DeviceCommand::getParameters).orElse(null),
                online,
                secondsSinceLastSeen
        ));
    }
}

