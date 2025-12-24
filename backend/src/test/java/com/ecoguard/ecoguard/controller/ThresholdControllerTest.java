package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.config.AuthInterceptor;
import com.ecoguard.ecoguard.config.AuthTokenService;
import com.ecoguard.ecoguard.dto.ThresholdAuditResponse;
import com.ecoguard.ecoguard.dto.ThresholdPayload;
import com.ecoguard.ecoguard.entity.DeviceCommand;
import com.ecoguard.ecoguard.entity.MetricType;
import com.ecoguard.ecoguard.entity.Role;
import com.ecoguard.ecoguard.entity.Threshold;
import com.ecoguard.ecoguard.entity.ThresholdAudit;
import com.ecoguard.ecoguard.repository.DeviceCommandRepository;
import com.ecoguard.ecoguard.repository.ThresholdAuditRepository;
import com.ecoguard.ecoguard.repository.ThresholdRepository;
import jakarta.servlet.http.HttpServletRequest;
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
 * Unit tests for ThresholdController.
 */
@ExtendWith(MockitoExtension.class)
class ThresholdControllerTest {

    @Mock
    private ThresholdRepository thresholdRepository;

    @Mock
    private ThresholdAuditRepository thresholdAuditRepository;

    @Mock
    private DeviceCommandRepository deviceCommandRepository;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ThresholdController thresholdController;

    private Threshold testThreshold;

    @BeforeEach
    void setUp() {
        testThreshold = new Threshold();
        testThreshold.setId(1L);
        testThreshold.setMetricType(MetricType.TEMP);
        testThreshold.setMinValue(new BigDecimal("10.0"));
        testThreshold.setMaxValue(new BigDecimal("30.0"));
    }

    @Test
    void testGetAllThresholds_Success() {
        Threshold threshold2 = new Threshold();
        threshold2.setId(2L);
        threshold2.setMetricType(MetricType.HUMIDITY);
        threshold2.setMinValue(new BigDecimal("20.0"));
        threshold2.setMaxValue(new BigDecimal("80.0"));

        List<Threshold> thresholds = Arrays.asList(testThreshold, threshold2);
        when(thresholdRepository.findAll()).thenReturn(thresholds);

        List<Threshold> result = thresholdController.getAllThresholds();

        assertEquals(2, result.size());
        verify(thresholdRepository, times(1)).findAll();
    }

    @Test
    void testGetThresholdById_Success() {
        when(thresholdRepository.findById(1L)).thenReturn(Optional.of(testThreshold));

        ResponseEntity<Threshold> response = thresholdController.getThresholdById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testThreshold.getId(), response.getBody().getId());
        verify(thresholdRepository, times(1)).findById(1L);
    }

    @Test
    void testGetThresholdById_NotFound() {
        when(thresholdRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Threshold> response = thresholdController.getThresholdById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    void testGetByMetric_Success() {
        when(thresholdRepository.findByMetricType(MetricType.TEMP)).thenReturn(Optional.of(testThreshold));

        ResponseEntity<Threshold> response = thresholdController.getByMetric(MetricType.TEMP);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(MetricType.TEMP, response.getBody().getMetricType());
        verify(thresholdRepository, times(1)).findByMetricType(MetricType.TEMP);
    }

    @Test
    void testGetByMetric_NotFound() {
        when(thresholdRepository.findByMetricType(MetricType.CO2)).thenReturn(Optional.empty());

        ResponseEntity<Threshold> response = thresholdController.getByMetric(MetricType.CO2);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testUpdate_Success() {
        ThresholdPayload payload = new ThresholdPayload();
        payload.setMinValue(new BigDecimal("15.0"));
        payload.setMaxValue(new BigDecimal("35.0"));

        AuthTokenService.Session session = new AuthTokenService.Session("admin", Role.ADMIN, java.time.Instant.now());
        when(request.getAttribute(AuthInterceptor.ATTR_SESSION)).thenReturn(session);
        when(thresholdRepository.findById(1L)).thenReturn(Optional.of(testThreshold));
        when(thresholdRepository.save(any(Threshold.class))).thenReturn(testThreshold);
        when(thresholdAuditRepository.save(any(ThresholdAudit.class))).thenReturn(new ThresholdAudit());
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenReturn(new DeviceCommand());

        ResponseEntity<?> response = thresholdController.update(1L, payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(thresholdRepository, times(1)).save(any(Threshold.class));
        verify(thresholdAuditRepository, times(1)).save(any(ThresholdAudit.class));
        verify(deviceCommandRepository, times(1)).save(any(DeviceCommand.class));
    }

    @Test
    void testUpdate_OnlyMinValue() {
        ThresholdPayload payload = new ThresholdPayload();
        payload.setMinValue(new BigDecimal("15.0"));

        AuthTokenService.Session session = new AuthTokenService.Session("admin", Role.ADMIN, java.time.Instant.now());
        when(request.getAttribute(AuthInterceptor.ATTR_SESSION)).thenReturn(session);
        when(thresholdRepository.findById(1L)).thenReturn(Optional.of(testThreshold));
        when(thresholdRepository.save(any(Threshold.class))).thenReturn(testThreshold);
        when(thresholdAuditRepository.save(any(ThresholdAudit.class))).thenReturn(new ThresholdAudit());
        when(deviceCommandRepository.save(any(DeviceCommand.class))).thenReturn(new DeviceCommand());

        ResponseEntity<?> response = thresholdController.update(1L, payload, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(thresholdRepository, times(1)).save(any(Threshold.class));
    }

    @Test
    void testUpdate_NotFound() {
        ThresholdPayload payload = new ThresholdPayload();
        payload.setMinValue(new BigDecimal("15.0"));

        when(thresholdRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = thresholdController.update(999L, payload, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(thresholdRepository, never()).save(any());
    }

    @Test
    void testRecentAudits_Success() {
        ThresholdAudit audit1 = new ThresholdAudit();
        audit1.setId(1L);
        audit1.setThresholdId(1L);
        audit1.setMetricType(MetricType.TEMP);
        audit1.setMinValue(new BigDecimal("10.0"));
        audit1.setMaxValue(new BigDecimal("30.0"));
        audit1.setUpdatedBy("admin");
        audit1.setUpdatedAt(LocalDateTime.now());

        ThresholdAudit audit2 = new ThresholdAudit();
        audit2.setId(2L);
        audit2.setThresholdId(2L);
        audit2.setMetricType(MetricType.HUMIDITY);
        audit2.setMinValue(new BigDecimal("20.0"));
        audit2.setMaxValue(new BigDecimal("80.0"));
        audit2.setUpdatedBy("admin");
        audit2.setUpdatedAt(LocalDateTime.now());

        List<ThresholdAudit> audits = Arrays.asList(audit1, audit2);
        when(thresholdAuditRepository.findTop10ByOrderByUpdatedAtDesc()).thenReturn(audits);

        List<ThresholdAuditResponse> result = thresholdController.recentAudits();

        assertEquals(2, result.size());
        verify(thresholdAuditRepository, times(1)).findTop10ByOrderByUpdatedAtDesc();
    }

    @Test
    void testDelete_Success() {
        doNothing().when(thresholdRepository).deleteById(1L);

        ResponseEntity<Void> response = thresholdController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(thresholdRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDelete_ExceptionHandled() {
        doThrow(new RuntimeException("Database error")).when(thresholdRepository).deleteById(1L);

        ResponseEntity<Void> response = thresholdController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

