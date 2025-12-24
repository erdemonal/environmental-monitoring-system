package com.ecoguard.ecoguard.controller;

import com.ecoguard.ecoguard.repository.SensorDataRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HealthController.
 */
@ExtendWith(MockitoExtension.class)
class HealthControllerTest {

    @Mock
    private SensorDataRepository sensorDataRepository;

    @InjectMocks
    private HealthController healthController;

    @Test
    void testHealth_Success() {
        when(sensorDataRepository.count()).thenReturn(100L);

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertNotNull(response.getBody().get("time"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> db = (Map<String, Object>) response.getBody().get("db");
        assertNotNull(db);
        assertEquals("UP", db.get("status"));
        assertEquals(100L, db.get("sensorDataCount"));
        
        verify(sensorDataRepository, times(1)).count();
    }

    @Test
    void testHealth_DatabaseError() {
        when(sensorDataRepository.count()).thenThrow(new RuntimeException("Database connection failed"));

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> db = (Map<String, Object>) response.getBody().get("db");
        assertNotNull(db);
        assertEquals("DOWN", db.get("status"));
        assertNotNull(db.get("error"));
        
        verify(sensorDataRepository, times(1)).count();
    }

    @Test
    void testHealth_ZeroCount() {
        when(sensorDataRepository.count()).thenReturn(0L);

        ResponseEntity<Map<String, Object>> response = healthController.health();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> db = (Map<String, Object>) response.getBody().get("db");
        assertEquals("UP", db.get("status"));
        assertEquals(0L, db.get("sensorDataCount"));
    }
}

