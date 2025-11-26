package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.dto.DeviceCommandPayload;
import com.ecoguard.ecoguard.entity.DeviceCommand;
import com.ecoguard.ecoguard.repository.DeviceCommandRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/device/commands")
public class AdminDeviceCommandController {

    private final DeviceCommandRepository deviceCommandRepository;

    public AdminDeviceCommandController(DeviceCommandRepository deviceCommandRepository) {
        this.deviceCommandRepository = deviceCommandRepository;
    }

    @PostMapping
    public ResponseEntity<?> sendCommand(@RequestBody DeviceCommandPayload payload) {
        if (payload.getDeviceKey() == null || payload.getCommandType() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "deviceKey and commandType required"));
        }

        DeviceCommand command = new DeviceCommand();
        command.setDeviceKey(payload.getDeviceKey());
        command.setCommandType(payload.getCommandType());
        command.setParameters(payload.getParameters());
        command.setExecuted(false);

        DeviceCommand saved = deviceCommandRepository.save(command);
        return ResponseEntity.ok(Map.of(
                "commandId", saved.getId(),
                "message", "Command sent to device"
        ));
    }

    @GetMapping
    public List<DeviceCommand> getAllCommands() {
        return deviceCommandRepository.findAll();
    }

    @GetMapping("/by-device/{deviceKey}")
    public List<DeviceCommand> getCommandsByDevice(@PathVariable String deviceKey) {
        return deviceCommandRepository.findByDeviceKeyAndExecutedFalseOrderByCreatedAtAsc(deviceKey);
    }
}

