package com.ecoguard.ecoguard.integration;

import com.ecoguard.ecoguard.entity.Role;
import com.ecoguard.ecoguard.entity.SensorData;
import com.ecoguard.ecoguard.entity.User;
import com.ecoguard.ecoguard.repository.SensorDataRepository;
import com.ecoguard.ecoguard.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for HealthController endpoint.
 * <p>
 * This test verifies the complete health check flow from HTTP request to database:
 * - HTTP layer (MockMvc) - sends GET request with authentication
 * - Authentication layer (AuthInterceptor) - validates Bearer token
 * - Controller layer (HealthController) - processes request
 * - Repository layer (SensorDataRepository) - queries database
 * - Database (H2) - returns actual data
 * <p>
 * Unlike unit tests that mock dependencies, this integration test uses real components
 * to ensure they work together correctly. This is a perfect example because:
 * 1. It tests a real endpoint that exists in production
 * 2. It verifies the full authentication flow (user creation → login → token usage)
 * 3. It verifies the full stack works together (HTTP → Auth → Controller → Repository → DB)
 * 4. It's easy to understand and explain
 * <p>
 * The test sets up authentication by creating a test user, logging in to obtain a token,
 * and then using that token to authenticate requests to the health endpoint.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HealthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up test data - @Transactional ensures rollback after each test
        sensorDataRepository.deleteAll();
        userRepository.deleteAll();

        // Create a test user and get auth token for authenticated requests
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash(passwordEncoder.encode("testpass"));
        testUser.setRole(Role.USER);
        userRepository.save(testUser);

        // Login to get token
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "testuser");
        loginRequest.put("password", "testpass");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<?, ?> response = objectMapper.readValue(loginResponse, Map.class);
        authToken = (String) response.get("token");
    }

    @Test
    void testHealth_WithData() throws Exception {
        // Arrange: Create some sensor data in the database
        SensorData data1 = new SensorData();
        data1.setTemperature(new BigDecimal("25.5"));
        data1.setTimestamp(LocalDateTime.now());
        sensorDataRepository.save(data1);

        SensorData data2 = new SensorData();
        data2.setTemperature(new BigDecimal("26.0"));
        data2.setTimestamp(LocalDateTime.now());
        sensorDataRepository.save(data2);

        // Act & Assert: Call health endpoint with authentication and verify response
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.time").exists())
                .andExpect(jsonPath("$.db.status").value("UP"))
                .andExpect(jsonPath("$.db.sensorDataCount").value(2));
    }

    @Test
    void testHealth_EmptyDatabase() throws Exception {
        // Arrange: No data in database (already cleaned in setUp)

        // Act & Assert: Call health endpoint with authentication and verify it still works
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.time").exists())
                .andExpect(jsonPath("$.db.status").value("UP"))
                .andExpect(jsonPath("$.db.sensorDataCount").value(0));
    }

    @Test
    void testHealth_ResponseStructure() throws Exception {
        // Arrange: Add one sensor data entry
        SensorData data = new SensorData();
        data.setTemperature(new BigDecimal("20.0"));
        data.setHumidity(new BigDecimal("50.0"));
        data.setTimestamp(LocalDateTime.now());
        sensorDataRepository.save(data);

        // Act & Assert: Verify the complete response structure with authentication
        mockMvc.perform(get("/api/health")
                        .header("Authorization", "Bearer " + authToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.time").exists())
                .andExpect(jsonPath("$.db").exists())
                .andExpect(jsonPath("$.db.status").exists())
                .andExpect(jsonPath("$.db.sensorDataCount").exists())
                .andExpect(jsonPath("$.db.error").doesNotExist()); // No error when DB is healthy
    }
}

