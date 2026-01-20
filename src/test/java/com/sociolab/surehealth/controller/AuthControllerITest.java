package com.sociolab.surehealth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerPatient_success() throws Exception {

        UserRegisterRequest request = new UserRegisterRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPassword("password123");


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerPatient_duplicateEmail() throws Exception {

        UserRegisterRequest request = new UserRegisterRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPassword("password123");


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorcode").value("DUPLICATE_EMAIL"))
                .andExpect(jsonPath("$.message").value("Email already exists"));
        ;
    }

    @Test
    void login_success() throws Exception {

        UserRegisterRequest request = new UserRegisterRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPassword("password123");


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("john@test.com",
                "password123");


        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

    }
    @Test
    void login_invalidPassword() throws Exception {

        UserRegisterRequest request = new UserRegisterRequest();
        request.setName("John");
        request.setEmail("john@test.com");
        request.setPassword("password123");


        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest("john@test.com",
                "wrong");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }


}

