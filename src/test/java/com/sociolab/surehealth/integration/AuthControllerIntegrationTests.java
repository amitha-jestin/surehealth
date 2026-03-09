package com.sociolab.surehealth.integration;

// ...existing code...

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
public class AuthControllerIntegrationTests extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void login_success_returns200_andToken() throws Exception {
        // Arrange - create a patient in DB with known password
        String rawPassword = "TestPass123!";
        User user = testDataFactory.createPatient(rawPassword);

        LoginRequest req = new LoginRequest(user.getEmail(), rawPassword);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.id").value(user.getId().intValue()));
    }

    @Test
    void login_invalidCredentials_returns401() throws Exception {
        // Arrange - create a patient but use wrong password
        User user = testDataFactory.createPatient("RightPassword1");

        LoginRequest req = new LoginRequest(user.getEmail(), "WrongPassword");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}
