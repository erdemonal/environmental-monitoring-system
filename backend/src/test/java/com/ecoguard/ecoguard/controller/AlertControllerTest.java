package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.entity.Alert;
import com.ecoguard.ecoguard.entity.AlertType;
import com.ecoguard.ecoguard.repository.AlertRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertController.
 */
@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertController alertController;

    private Alert testAlert;

    @BeforeEach
    void setUp() {
        testAlert = new Alert();
        testAlert.setId(1L);
        testAlert.setAlertType(AlertType.THRESHOLD);
        testAlert.setMetricType("TEMP");
        testAlert.setValue(new BigDecimal("25.5"));
        testAlert.setTimestamp(LocalDateTime.now());
        testAlert.setAcknowledged(false);
    }

    @Test
    void testGetAllAlerts_Success() {
        Alert alert2 = new Alert();
        alert2.setId(2L);
        alert2.setAlertType(AlertType.INTRUDER);
        alert2.setMetricType("LIGHT");
        alert2.setValue(new BigDecimal("1000"));
        alert2.setTimestamp(LocalDateTime.now());
        alert2.setAcknowledged(false);

        List<Alert> alerts = Arrays.asList(testAlert, alert2);
        when(alertRepository.findAll()).thenReturn(alerts);

        List<Alert> result = alertController.getAllAlerts();

        assertEquals(2, result.size());
        assertEquals(testAlert.getId(), result.get(0).getId());
        assertEquals(alert2.getId(), result.get(1).getId());
        verify(alertRepository, times(1)).findAll();
    }

    @Test
    void testGetAllAlerts_EmptyList() {
        when(alertRepository.findAll()).thenReturn(List.of());

        List<Alert> result = alertController.getAllAlerts();

        assertTrue(result.isEmpty());
        verify(alertRepository, times(1)).findAll();
    }

    @Test
    void testGetAlertById_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));

        ResponseEntity<Alert> response = alertController.getAlertById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testAlert.getId(), response.getBody().getId());
        assertEquals(testAlert.getMetricType(), response.getBody().getMetricType());
        verify(alertRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAlertById_NotFound() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Alert> response = alertController.getAlertById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(alertRepository, times(1)).findById(999L);
    }

    @Test
    void testAcknowledgeAlert_Success() {
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        ResponseEntity<?> response = alertController.acknowledgeAlert(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(testAlert.getAcknowledged());
        verify(alertRepository, times(1)).findById(1L);
        verify(alertRepository, times(1)).save(testAlert);
    }

    @Test
    void testAcknowledgeAlert_NotFound() {
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = alertController.acknowledgeAlert(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(alertRepository, times(1)).findById(999L);
        verify(alertRepository, never()).save(any());
    }
}

