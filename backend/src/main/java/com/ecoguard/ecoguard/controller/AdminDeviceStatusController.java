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

import java.util.Optional;

@RestController
@RequestMapping("/api/admin/device/status")
public class AdminDeviceStatusController {

    private final SensorDataRepository sensorDataRepository;
    private final AlertRepository alertRepository;
    private final DeviceCommandRepository deviceCommandRepository;

    public AdminDeviceStatusController(SensorDataRepository sensorDataRepository,
                                       AlertRepository alertRepository,
                                       DeviceCommandRepository deviceCommandRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.alertRepository = alertRepository;
        this.deviceCommandRepository = deviceCommandRepository;
    }

    @GetMapping
    public ResponseEntity<DeviceStatusResponse> getStatus() {
        Optional<SensorData> latestData = sensorDataRepository.findTopByOrderByTimestampDesc();
        Optional<Alert> latestAlert = alertRepository.findTopByOrderByTimestampDesc();
        Optional<DeviceCommand> latestMessage = deviceCommandRepository.findTopByCommandTypeOrderByCreatedAtDesc("DISPLAY_MESSAGE");

        SensorData data = latestData.orElse(null);
        return ResponseEntity.ok(new DeviceStatusResponse(
                data != null ? data.getTimestamp() : null,
                data != null ? data.getIpAddress() : null,
                data != null ? data.getTemperature() : null,
                data != null ? data.getHumidity() : null,
                data != null ? data.getCo2Level() : null,
                data != null ? data.getLightLevel() : null,
                latestAlert.map(Alert::getTimestamp).orElse(null),
                latestAlert.map(Alert::getAlertType).map(Enum::name).orElse(null),
                latestMessage.map(DeviceCommand::getParameters).orElse(null)
        ));
    }
}

