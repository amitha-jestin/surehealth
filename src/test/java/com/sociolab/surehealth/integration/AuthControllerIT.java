package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.RefreshTokenRequest;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.service.RefreshTokenHasher;
import com.sociolab.surehealth.service.RedisService;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT extends TestContainersConfig {

    private static final String BASE = "/api/v1/auth";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private RedisService redisService;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    private User user;
    private String rawPassword;

    @BeforeEach
    void setup() {
        rawPassword = "TestPass123!";
        user = testDataFactory.createPatient(rawPassword);
    }

    @AfterEach
    void cleanup() {
        redisService.clearAll();
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("shouldLoginSuccessfully")
        void shouldLoginSuccessfully() throws Exception {
            LoginRequest req = new LoginRequest(user.getEmail(), rawPassword);

            MvcResult res = mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.token").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                    .andExpect(jsonPath("$.data.id").value(user.getId().intValue()))
                    .andReturn();

            JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
            String refreshToken = root.path("data").path("refreshToken").asText();
            String stored = redisService.getRefreshToken(user.getId());

            assertNotNull(stored);
            assertEquals(refreshTokenHasher.hash(refreshToken), stored);
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenInvalidCredentials")
        void shouldReturnUnauthorizedWhenInvalidCredentials() throws Exception {
            LoginRequest req = new LoginRequest(user.getEmail(), "WrongPass1!");

            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenValidationFails")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMalformedJson")
        void shouldReturnBadRequestWhenMalformedJson() throws Exception {
            mockMvc.perform(post(BASE + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("shouldLogoutSuccessfully")
        void shouldLogoutSuccessfully() throws Exception {
            String accessToken = loginAndGetAccessToken();

            mockMvc.perform(post(BASE + "/logout")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meta.message").value("Logout successful"));

            boolean blacklisted = redisService.isTokenBlacklisted(accessToken);
            assertEquals(true, blacklisted);
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            mockMvc.perform(post(BASE + "/logout"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Refresh")
    class RefreshTests {

        @Test
        @DisplayName("shouldRefreshTokenSuccessfully")
        void shouldRefreshTokenSuccessfully() throws Exception {
            String refreshToken = loginAndGetRefreshToken();
            RefreshTokenRequest req = new RefreshTokenRequest(refreshToken);

            MvcResult res = mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andReturn();

            JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
            String newRefresh = root.path("data").path("refreshToken").asText();
            String stored = redisService.getRefreshToken(user.getId());

            assertNotNull(stored);
            assertEquals(refreshTokenHasher.hash(newRefresh), stored);
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenInvalidToken")
        void shouldReturnUnauthorizedWhenInvalidToken() throws Exception {
            RefreshTokenRequest req = new RefreshTokenRequest("invalid-token");

            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenValidationFails")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMalformedJson")
        void shouldReturnBadRequestWhenMalformedJson() throws Exception {
            mockMvc.perform(post(BASE + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());
        }
    }

    private String loginAndGetAccessToken() throws Exception {
        LoginRequest req = new LoginRequest(user.getEmail(), rawPassword);

        MvcResult res = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }

    private String loginAndGetRefreshToken() throws Exception {
        LoginRequest req = new LoginRequest(user.getEmail(), rawPassword);

        MvcResult res = mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        return root.path("data").path("refreshToken").asText();
    }
}
