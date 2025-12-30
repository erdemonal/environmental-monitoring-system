package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.dto.DeviceCommandResponse;
import com.ecoguard.ecoguard.dto.DeviceConfigResponse;
import com.ecoguard.ecoguard.dto.SensorDataPayload;
import com.ecoguard.ecoguard.dto.ThresholdDeviceResponse;
import com.ecoguard.ecoguard.entity.Alert;
import com.ecoguard.ecoguard.entity.AlertType;
import com.ecoguard.ecoguard.entity.DeviceCommand;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.SensorData;
import com.ecoguard.ecoguard.entity.Notification;
import com.ecoguard.ecoguard.repository.AlertRepository;
import com.ecoguard.ecoguard.repository.DeviceCommandRepository;
import com.ecoguard.ecoguard.repository.DeviceConfigRepository;
import com.ecoguard.ecoguard.repository.NotificationRepository;
import com.ecoguard.ecoguard.repository.SensorDataRepository;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import com.ecoguard.ecoguard.repository.UserRepository;
import com.ecoguard.ecoguard.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST controller for ESP32 device communication.
 * <p>
 * Handles sensor data ingestion from the device, threshold retrieval, and command
 * management. This is the primary interface between the ESP32 and the backend.
 * All endpoints require the X-Device-Key header for authentication.
 *
 * @author EcoGuard 
 * @since 1.0
 */
@RestController
@RequestMapping("/api/device")
public class DeviceSensorController {

    private static final Logger logger = LoggerFactory.getLogger(DeviceSensorController.class);

    private final SensorDataRepository sensorDataRepository;
    private final ThresholdRepository thresholdRepository;
    private final AlertRepository alertRepository;
    private final DeviceCommandRepository deviceCommandRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;
    private final NotificationRepository notificationRepository;
    private final DeviceConfigRepository deviceConfigRepository;

    /**
     * Constructs a new DeviceSensorController with required dependencies.
     *
     * @param sensorDataRepository repository for sensor data persistence
     * @param thresholdRepository repository for threshold retrieval
     * @param alertRepository repository for alert creation
     * @param deviceCommandRepository repository for command management
     * @param userRepository repository for user operations
     * @param pushNotificationService service for sending push notifications
     * @param notificationRepository repository for notification persistence
     * @param deviceConfigRepository repository for device configuration
     */
    public DeviceSensorController(SensorDataRepository sensorDataRepository,
                                  ThresholdRepository thresholdRepository,
                                  AlertRepository alertRepository,
                                  DeviceCommandRepository deviceCommandRepository,
                                  UserRepository userRepository,
                                  PushNotificationService pushNotificationService,
                                  NotificationRepository notificationRepository,
                                  DeviceConfigRepository deviceConfigRepository) {
        this.sensorDataRepository = sensorDataRepository;
        this.thresholdRepository = thresholdRepository;
        this.alertRepository = alertRepository;
        this.deviceCommandRepository = deviceCommandRepository;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
        this.notificationRepository = notificationRepository;
        this.deviceConfigRepository = deviceConfigRepository;
    }

    /**
     * Ingests sensor data from the ESP32 device.
     * <p>
     * Accepts sensor readings (temperature, humidity, CO2, light) and:
     * <ul>
     *   <li>Persists the data to the database</li>
     *   <li>Evaluates each metric against configured thresholds</li>
     *   <li>Creates alerts for any threshold breaches</li>
     * </ul>
     *
     * @param payload the sensor data payload from the device
     * @return ResponseEntity containing the saved sensor data ID and list of created alert IDs
     */
    @PostMapping("/sensor-data")
    public ResponseEntity<?> ingest(@RequestBody SensorDataPayload payload) {
        if (payload == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Payload required"));
        }
        boolean hasValue = payload.getTemperature() != null
                || payload.getHumidity() != null
                || payload.getCo2Level() != null
                || payload.getLightLevel() != null;
        if (!hasValue) {
            return ResponseEntity.badRequest().body(Map.of("message", "At least one metric (temperature, humidity, co2Level, lightLevel) required"));
        }

        SensorData data = new SensorData();
        data.setTemperature(payload.getTemperature());
        data.setHumidity(payload.getHumidity());
        data.setCo2Level(payload.getCo2Level());
        data.setLightLevel(payload.getLightLevel());
        if (payload.getTimestamp() != null) {
            data.setTimestamp(payload.getTimestamp());
        } else {
            data.setTimestamp(LocalDateTime.now());
        }
        SensorData saved = sensorDataRepository.save(data);

        List<Alert> alerts = new ArrayList<>();
        evaluateMetric(MetricType.TEMP, toBigDecimal(payload.getTemperature()), alerts);
        evaluateMetric(MetricType.HUMIDITY, toBigDecimal(payload.getHumidity()), alerts);
        evaluateMetric(MetricType.CO2, toBigDecimal(payload.getCo2Level()), alerts);
        evaluateMetric(MetricType.LIGHT, toBigDecimal(payload.getLightLevel()), alerts);

        return ResponseEntity.ok(Map.of(
                "sensorDataId", saved.getId(),
                "alertsCreated", alerts.stream().map(Alert::getId).toList()
        ));
    }

    /**
     * Retrieves all threshold configurations for the device.
     * <p>
     * Called by the ESP32 device to fetch current threshold values for local
     * evaluation. Returns thresholds in a format suitable for device processing.
     *
     * @return list of threshold configurations
     */
    @GetMapping("/thresholds")
    public List<ThresholdDeviceResponse> getThresholds() {
        return thresholdRepository.findAll().stream()
                .map(threshold -> new ThresholdDeviceResponse(
                        threshold.getMetricType().name(),
                        threshold.getMinValue(),
                        threshold.getMaxValue()
                ))
                .toList();
    }

    /**
     * Retrieves pending commands for the device.
     * <p>
     * Called periodically by the ESP32 to check for new commands from administrators.
     * Returns only unexecuted commands, ordered by creation time (oldest first).
     *
     * @param deviceKey the device key from the X-Device-Key header
     * @return list of pending commands for the device
     */
    @GetMapping("/commands")
    public List<DeviceCommandResponse> getCommands(@RequestHeader("X-Device-Key") String deviceKey) {
        List<DeviceCommand> commands = deviceCommandRepository.findByDeviceKeyAndExecutedFalseOrderByCreatedAtAsc(deviceKey);
        return commands.stream()
                .map(cmd -> {
                    DeviceCommandResponse resp = new DeviceCommandResponse();
                    resp.setId(cmd.getId());
                    resp.setCommandType(cmd.getCommandType());
                    resp.setParameters(cmd.getParameters());
                    return resp;
                })
                .toList();
    }

    /**
     * Acknowledges that a command has been executed by the device.
     * <p>
     * Called by the ESP32 after successfully executing a command. Marks the command
     * as executed and records the execution timestamp.
     *
     * @param deviceKey the device key from the X-Device-Key header
     * @param id the command ID to acknowledge
     * @return ResponseEntity with success message, or 404 Not Found if command doesn't exist
     */
    @PutMapping("/commands/{id}/ack")
    public ResponseEntity<?> acknowledgeCommand(@RequestHeader("X-Device-Key") String deviceKey, @PathVariable("id") Long id) {
        return deviceCommandRepository.findByIdAndDeviceKey(id, deviceKey)
                .map(cmd -> {
                    cmd.setExecuted(true);
                    cmd.setExecutedAt(LocalDateTime.now());
                    deviceCommandRepository.save(cmd);
                    return ResponseEntity.ok(Map.of("message", "Command acknowledged"));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieves device configuration for the authenticated device.
     * <p>
     * Called by the ESP32 device to fetch its network configuration (WiFi credentials
     * and backend connection info). Returns configuration if available, or 404 if not found.
     * This allows devices to retrieve configuration remotely instead of hardcoding.
     *
     * @param deviceKey the device key from the X-Device-Key header
     * @return ResponseEntity containing device configuration, or 404 Not Found if not configured
     */
    @GetMapping("/config")
    public ResponseEntity<DeviceConfigResponse> getConfig(@RequestHeader("X-Device-Key") String deviceKey) {
        return deviceConfigRepository.findByDeviceKey(deviceKey)
                .map(config -> ResponseEntity.ok(new DeviceConfigResponse(
                        config.getDeviceKey(),
                        config.getWifiSsid(),
                        config.getWifiPassword(),
                        config.getBackendHost(),
                        config.getBackendPort(),
                        config.getUpdatedAt()
                )))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    /**
     * Evaluates a sensor value against its threshold and creates an alert if breached.
     *
     * @param metric the metric type to evaluate
     * @param value the sensor value to check
     * @param alerts list to add created alerts to
     */
    private void evaluateMetric(MetricType metric, BigDecimal value, List<Alert> alerts) {
        if (value == null) {
            return;
        }
        thresholdRepository.findByMetricType(metric).ifPresent(threshold -> {
            if (value.compareTo(threshold.getMinValue()) < 0 || value.compareTo(threshold.getMaxValue()) > 0) {
                Alert alert = new Alert();
                alert.setAlertType(AlertType.THRESHOLD);
                alert.setMetricType(metric.name());
                alert.setValue(value);
                alert.setTimestamp(LocalDateTime.now());
                Alert savedAlert = alertRepository.save(alert);
                alerts.add(savedAlert);
                notifyUsers(savedAlert);
            }
        });
    }

    /**
     * Sends a push notification to all users with a registered device token.
     * Also persists notification records to the database for tracking.
     *
     * @param alert the alert that was created
     */
    private void notifyUsers(Alert alert) {
        String title = "EcoGuard Alert: " + alert.getMetricType();
        String body = "Value " + alert.getValue() + " is outside thresholds.";
        logger.info("Notifying users about alert: {} | {}", title, body);
        userRepository.findByDeviceTokenIsNotNull()
                .forEach(user -> {
                    // Send push notification
                    pushNotificationService.sendPushNotification(
                            user.getDeviceToken(),
                            title,
                            body
                    );
                    // Save notification record to database
                    Notification notification = new Notification();
                    notification.setUser(user);
                    notification.setAlert(alert);
                    notificationRepository.save(notification);
                });
    }

    /**
     * Converts a BigDecimal to BigDecimal (no-op, for method overloading).
     *
     * @param value the BigDecimal value
     * @return the same BigDecimal value
     */
    private BigDecimal toBigDecimal(BigDecimal value) {
        return value;
    }

    /**
     * Converts an Integer to BigDecimal.
     *
     * @param value the Integer value to convert
     * @return BigDecimal representation, or null if input is null
     */
    private BigDecimal toBigDecimal(Integer value) {
        return value == null ? null : new BigDecimal(value);
    }

}

