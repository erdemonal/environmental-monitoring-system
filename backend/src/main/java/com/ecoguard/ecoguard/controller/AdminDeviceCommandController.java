package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.dto.DeviceCommandPayload;
import com.ecoguard.ecoguard.entity.DeviceCommand;
import com.ecoguard.ecoguard.repository.DeviceCommandRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing device commands (admin only).
 * <p>
 * Provides endpoints for sending commands to the ESP32 device, viewing command history,
 * and checking pending commands. Commands are queued and fetched by the device periodically.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/admin/device/commands")
public class AdminDeviceCommandController {

    private final DeviceCommandRepository deviceCommandRepository;

    /**
     * Constructs a new AdminDeviceCommandController with required dependencies.
     *
     * @param deviceCommandRepository repository for command persistence
     */
    public AdminDeviceCommandController(DeviceCommandRepository deviceCommandRepository) {
        this.deviceCommandRepository = deviceCommandRepository;
    }

    /**
     * Sends a command to the ESP32 device.
     * <p>
     * Creates a new command record that will be fetched by the device on its next
     * command check cycle. Supported command types: SET_LED_COLOR, DISPLAY_MESSAGE,
     * BLE_BROADCAST, REFRESH_CONFIG.
     *
     * @param payload the command payload containing deviceKey, commandType, and optional parameters
     * @return ResponseEntity with command ID and success message, or 400 Bad Request if validation fails
     */
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

    /**
     * Retrieves all commands in the system.
     *
     * @return list of all device commands (executed and pending)
     */
    @GetMapping
    public List<DeviceCommand> getAllCommands() {
        return deviceCommandRepository.findAll();
    }

    /**
     * Retrieves pending (unexecuted) commands for a specific device.
     *
     * @param deviceKey the device key to filter by
     * @return list of pending commands for the device, ordered by creation time
     */
    @GetMapping("/by-device/{deviceKey}")
    public List<DeviceCommand> getCommandsByDevice(@PathVariable String deviceKey) {
        return deviceCommandRepository.findByDeviceKeyAndExecutedFalseOrderByCreatedAtAsc(deviceKey);
    }

    /**
     * Retrieves command history for a specific device.
     * <p>
     * Returns the last 10 commands (executed or pending) for the device,
     * ordered by creation time descending (most recent first).
     *
     * @param deviceKey the device key to filter by
     * @return list of the 10 most recent commands for the device
     */
    @GetMapping("/by-device/{deviceKey}/history")
    public List<DeviceCommand> getCommandHistory(@PathVariable("deviceKey") String deviceKey) {
        return deviceCommandRepository.findTop10ByDeviceKeyOrderByCreatedAtDesc(deviceKey);
    }
}

