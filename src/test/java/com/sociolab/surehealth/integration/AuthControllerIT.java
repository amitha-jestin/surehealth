package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.RefreshTokenRequest;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.service.RedisService;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class AuthControllerIT extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private RedisService redisService;

    // ================= CLEANUP =================

    @AfterEach
    void cleanup() {
        redisService.clearAll(); // prevent Redis state leakage
    }

    // ================= LOGIN SUCCESS =================

    @Test
    void login_success_returnsTokenAndStoresRefreshTokenInRedis() throws Exception {

        String rawPassword = "TestPass123!";
        User user = testDataFactory.createPatient(rawPassword);

        LoginRequest req = new LoginRequest(user.getEmail(), rawPassword);

        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value(user.getEmail()))
                .andExpect(jsonPath("$.data.id").value(user.getId().intValue()))
                .andReturn();

        JsonNode root = objectMapper.readTree(res.getResponse().getContentAsString());
        String refreshToken = root.path("data").path("refreshToken").asText();

        String stored = redisService.getRefreshToken(user.getId());

        assertThat(stored).isEqualTo(refreshToken);
    }

    // ================= LOGIN INVALID PASSWORD =================

    @Test
    void login_invalidCredentials_returns401() throws Exception {

        User user = testDataFactory.createPatient("CorrectPassword1");

        LoginRequest req = new LoginRequest(user.getEmail(), "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    // ================= LOGIN BLOCKED USER =================

    @Test
    void login_blockedUser_returns403() throws Exception {

        String rawPassword = "BlockedPass123!";
        User user = testDataFactory.createPatient(rawPassword);

        user.setStatus(AccountStatus.BLOCKED);

        LoginRequest req = new LoginRequest(user.getEmail(), rawPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // ================= LOGOUT =================

    @Test
    void logout_validToken_blacklistsTokenAndDeletesRefreshToken() throws Exception {

        String rawPassword = "LogoutPass123!";
        User user = testDataFactory.createPatient(rawPassword);

        LoginRequest loginReq = new LoginRequest(user.getEmail(), rawPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());

        String accessToken = root.path("data").path("token").asText();

        // ensure refresh token stored
        assertThat(redisService.getRefreshToken(user.getId())).isNotNull();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.message").value("Logout successful"));

        boolean blacklisted = redisService.isTokenBlacklisted(accessToken);
        assertThat(blacklisted).isTrue();

        String refreshAfter = redisService.getRefreshToken(user.getId());
        assertThat(refreshAfter).isNull();
    }

    // ================= REFRESH TOKEN SUCCESS =================

    @Test
    void refreshToken_validToken_returnsNewTokensAndRotatesRedisToken() throws Exception {

        String rawPassword = "RefreshPass123!";
        User user = testDataFactory.createPatient(rawPassword);

        LoginRequest loginReq = new LoginRequest(user.getEmail(), rawPassword);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginRoot = objectMapper.readTree(loginResult.getResponse().getContentAsString());

        String refreshToken = loginRoot.path("data").path("refreshToken").asText();

        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        JsonNode refreshRoot = objectMapper.readTree(refreshResult.getResponse().getContentAsString());

        String newRefresh = refreshRoot.path("data").path("refreshToken").asText();

        String stored = redisService.getRefreshToken(user.getId());

        assertThat(stored).isEqualTo(newRefresh);
    }

    // ================= REFRESH TOKEN INVALID =================

    @Test
    void refreshToken_invalidToken_returns401() throws Exception {

        RefreshTokenRequest req = new RefreshTokenRequest("invalid-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }
}