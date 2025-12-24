package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.entity.SensorData;
import com.ecoguard.ecoguard.repository.SensorDataRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SensorDataController.
 */
@ExtendWith(MockitoExtension.class)
class SensorDataControllerTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @InjectMocks
    private SensorDataController sensorDataController;

    private SensorData testSensorData;

    @BeforeEach
    void setUp() {
        testSensorData = new SensorData();
        testSensorData.setId(1L);
        testSensorData.setTemperature(new BigDecimal("25.5"));
        testSensorData.setHumidity(new BigDecimal("60.0"));
        testSensorData.setCo2Level(400);
        testSensorData.setLightLevel(500);
        testSensorData.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testGetAll_Success() {
        SensorData data2 = new SensorData();
        data2.setId(2L);
        data2.setTemperature(new BigDecimal("26.0"));
        data2.setTimestamp(LocalDateTime.now());

        List<SensorData> sensorDataList = Arrays.asList(testSensorData, data2);
        when(sensorDataRepository.findAll()).thenReturn(sensorDataList);

        List<SensorData> result = sensorDataController.getAll();

        assertEquals(2, result.size());
        assertEquals(testSensorData.getId(), result.get(0).getId());
        verify(sensorDataRepository, times(1)).findAll();
    }

    @Test
    void testGetAll_EmptyList() {
        when(sensorDataRepository.findAll()).thenReturn(List.of());

        List<SensorData> result = sensorDataController.getAll();

        assertTrue(result.isEmpty());
        verify(sensorDataRepository, times(1)).findAll();
    }

    @Test
    void testGetLatest_Success() {
        when(sensorDataRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.of(testSensorData));

        ResponseEntity<SensorData> response = sensorDataController.getLatest();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testSensorData.getId(), response.getBody().getId());
        verify(sensorDataRepository, times(1)).findTopByOrderByTimestampDesc();
    }

    @Test
    void testGetLatest_NotFound() {
        when(sensorDataRepository.findTopByOrderByTimestampDesc()).thenReturn(Optional.empty());

        ResponseEntity<SensorData> response = sensorDataController.getLatest();

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testGetRange_Success() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now();

        List<SensorData> sensorDataList = Arrays.asList(testSensorData);
        when(sensorDataRepository.findAllByTimestampBetween(start, end)).thenReturn(sensorDataList);

        List<SensorData> result = sensorDataController.getRange(start, end);

        assertEquals(1, result.size());
        verify(sensorDataRepository, times(1)).findAllByTimestampBetween(start, end);
    }

    @Test
    void testGetRange_EmptyResult() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now();

        when(sensorDataRepository.findAllByTimestampBetween(start, end)).thenReturn(List.of());

        List<SensorData> result = sensorDataController.getRange(start, end);

        assertTrue(result.isEmpty());
    }
}

