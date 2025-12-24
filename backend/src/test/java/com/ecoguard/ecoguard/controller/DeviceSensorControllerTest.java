package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.dto.DeviceCommandResponse;
import com.ecoguard.ecoguard.dto.SensorDataPayload;
import com.ecoguard.ecoguard.dto.ThresholdDeviceResponse;
import com.ecoguard.ecoguard.entity.*;
import com.ecoguard.ecoguard.repository.*;
import com.ecoguard.ecoguard.service.PushNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for DeviceSensorController.
 */
@ExtendWith(MockitoExtension.class)
class DeviceSensorControllerTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @Mock
    private ThresholdRepository thresholdRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private DeviceSensorController deviceSensorController;

    private SensorDataPayload testPayload;
    private Threshold testThreshold;

    @BeforeEach
    void setUp() {
        testPayload = new SensorDataPayload();
        testPayload.setTemperature(new BigDecimal("25.5"));
        testPayload.setHumidity(new BigDecimal("60.0"));
        testPayload.setCo2Level(400);
        testPayload.setLightLevel(500);

        testThreshold = new Threshold();
        testThreshold.setId(1L);
        testThreshold.setMetricType(MetricType.TEMP);
        testThreshold.setMinValue(new BigDecimal("10.0"));
        testThreshold.setMaxValue(new BigDecimal("30.0"));
    }

    @Test
    void testIngest_Success() {
        SensorData savedData = new SensorData();
        savedData.setId(1L);
        savedData.setTemperature(testPayload.getTemperature());
        savedData.setHumidity(testPayload.getHumidity());
        savedData.setCo2Level(testPayload.getCo2Level());
        savedData.setLightLevel(testPayload.getLightLevel());
        savedData.setTimestamp(LocalDateTime.now());

        when(sensorDataRepository.save(any(SensorData.class))).thenReturn(savedData);
        lenient().when(thresholdRepository.findByMetricType(MetricType.TEMP)).thenReturn(Optional.empty());
        lenient().when(thresholdRepository.findByMetricType(MetricType.HUMIDITY)).thenReturn(Optional.empty());
        lenient().when(thresholdRepository.findByMetricType(MetricType.CO2)).thenReturn(Optional.empty());
        lenient().when(thresholdRepository.findByMetricType(MetricType.LIGHT)).thenReturn(Optional.empty());
        lenient().when(userRepository.findByDeviceTokenIsNotNull()).thenReturn(List.of());

        ResponseEntity<?> response = deviceSensorController.ingest(testPayload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        verify(sensorDataRepository, times(1)).save(any(SensorData.class));
    }

    @Test
    void testIngest_NullPayload() {
        ResponseEntity<?> response = deviceSensorController.ingest(null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertEquals("Payload required", responseBody.get("message"));
    }

    @Test
    void testIngest_NoMetrics() {
        SensorDataPayload emptyPayload = new SensorDataPayload();

        ResponseEntity<?> response = deviceSensorController.ingest(emptyPayload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof Map);
        Map<?, ?> responseBody = (Map<?, ?>) response.getBody();
        assertTrue(responseBody.get("message").toString().contains("At least one metric"));
    }

    @Test
    void testIngest_ThresholdBreach() {
        SensorDataPayload breachPayload = new SensorDataPayload();
        breachPayload.setTemperature(new BigDecimal("35.0")); // Above max threshold

        SensorData savedData = new SensorData();
        savedData.setId(1L);
        savedData.setTemperature(breachPayload.getTemperature());
        savedData.setTimestamp(LocalDateTime.now());

        Threshold tempThreshold = new Threshold();
        tempThreshold.setMetricType(MetricType.TEMP);
        tempThreshold.setMinValue(new BigDecimal("10.0"));
        tempThreshold.setMaxValue(new BigDecimal("30.0"));

        when(sensorDataRepository.save(any(SensorData.class))).thenReturn(savedData);
        when(thresholdRepository.findByMetricType(MetricType.TEMP)).thenReturn(Optional.of(tempThreshold));
        // Use lenient for other metrics since they're checked but won't breach
        lenient().when(thresholdRepository.findByMetricType(MetricType.HUMIDITY)).thenReturn(Optional.empty());
        lenient().when(thresholdRepository.findByMetricType(MetricType.CO2)).thenReturn(Optional.empty());
        lenient().when(thresholdRepository.findByMetricType(MetricType.LIGHT)).thenReturn(Optional.empty());
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
            Alert alert = invocation.getArgument(0);
            alert.setId(1L);
            return alert;
        });
        when(userRepository.findByDeviceTokenIsNotNull()).thenReturn(List.of());

        ResponseEntity<?> response = deviceSensorController.ingest(breachPayload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(alertRepository, atLeastOnce()).save(any(Alert.class));
    }

    @Test
    void testGetThresholds_Success() {
        Threshold threshold1 = new Threshold();
        threshold1.setMetricType(MetricType.TEMP);
        threshold1.setMinValue(new BigDecimal("10.0"));
        threshold1.setMaxValue(new BigDecimal("30.0"));

        Threshold threshold2 = new Threshold();
        threshold2.setMetricType(MetricType.HUMIDITY);
        threshold2.setMinValue(new BigDecimal("20.0"));
        threshold2.setMaxValue(new BigDecimal("80.0"));

        List<Threshold> thresholds = Arrays.asList(threshold1, threshold2);
        when(thresholdRepository.findAll()).thenReturn(thresholds);

        List<ThresholdDeviceResponse> result = deviceSensorController.getThresholds();

        assertEquals(2, result.size());
        assertEquals("TEMP", result.get(0).metricType());
        assertEquals("HUMIDITY", result.get(1).metricType());
        verify(thresholdRepository, times(1)).findAll();
    }

    @Test
    void testGetCommands_Success() {
        DeviceCommand command1 = new DeviceCommand();
        command1.setId(1L);
        command1.setDeviceKey("demo-device-key");
        command1.setCommandType("SET_LED_COLOR");
        command1.setParameters("255,0,0");
        command1.setExecuted(false);
        command1.setCreatedAt(LocalDateTime.now());

        DeviceCommand command2 = new DeviceCommand();
        command2.setId(2L);
        command2.setDeviceKey("demo-device-key");
        command2.setCommandType("DISPLAY_MESSAGE");
        command2.setParameters("Hello");
        command2.setExecuted(false);
        command2.setCreatedAt(LocalDateTime.now());

        List<DeviceCommand> commands = Arrays.asList(command1, command2);
        when(deviceCommandRepository.findByDeviceKeyAndExecutedFalseOrderByCreatedAtAsc("demo-device-key"))
                .thenReturn(commands);

        List<DeviceCommandResponse> result = deviceSensorController.getCommands("demo-device-key");

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("SET_LED_COLOR", result.get(0).getCommandType());
        verify(deviceCommandRepository, times(1))
                .findByDeviceKeyAndExecutedFalseOrderByCreatedAtAsc("demo-device-key");
    }

    @Test
    void testAcknowledgeCommand_Success() {
        DeviceCommand command = new DeviceCommand();
        command.setId(1L);
        command.setDeviceKey("demo-device-key");
        command.setCommandType("SET_LED_COLOR");
        command.setExecuted(false);

        when(deviceCommandRepository.findByIdAndDeviceKey(1L, "demo-device-key"))
                .thenReturn(Optional.of(command));
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenReturn(command);

        ResponseEntity<?> response = deviceSensorController.acknowledgeCommand("demo-device-key", 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(command.getExecuted());
        assertNotNull(command.getExecutedAt());
        verify(deviceCommandRepository, times(1)).save(command);
    }

    @Test
    void testAcknowledgeCommand_NotFound() {
        when(deviceCommandRepository.findByIdAndDeviceKey(999L, "demo-device-key"))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = deviceSensorController.acknowledgeCommand("demo-device-key", 999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(deviceCommandRepository, never()).save(any());
    }
}

