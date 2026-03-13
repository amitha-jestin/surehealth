package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
public class UserControllerIT extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDataFactory testDataFactory;

    @Test
    void registerPatient_createsUserAndReturns201() throws Exception {
        String email = "patient+" + UUID.randomUUID() + "@example.com";

        UserRegisterRequest req = new UserRegisterRequest(
                "Integration Patient",
                email,
                "Strong@pass12"
        );

        mockMvc.perform(post("/api/v1/users/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value("Integration Patient"));

        Optional<User> saved = userRepository.findByEmail(email);
        assertTrue(saved.isPresent());
        assertThat(saved.get().getName()).isEqualTo("Integration Patient");
    }

    @Test
    void registerPatient_duplicateEmail_usingTestDataFactory_returnsConflict() throws Exception {
        // create an existing patient using the TestDataFactory
        User existing = testDataFactory.createPatient("Factory@ass123");

        // attempt to register a new patient with the same email
        UserRegisterRequest dupReq = new UserRegisterRequest(
                "Duplicate",
                existing.getEmail(),
                "Another@Pass1"
        );

        mockMvc.perform(post("/api/v1/users/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerDoctor_duplicateEmail_usingTestDataFactory_returnsConflict() throws Exception {
        // create an existing doctor using the TestDataFactory
        Doctor existing = testDataFactory.createDoctor("Factory@ass123");

        // attempt to register a new Doctor with the same email
        DoctorRegisterRequest dupReq = new DoctorRegisterRequest(
                "Integration Doctor",
                existing.getUser().getEmail(),
                "Doctor@pass12",
                com.sociolab.surehealth.enums.Speciality.CARDIOLOGY,
                "LIC-INT-123",
                5
        );

        mockMvc.perform(post("/api/v1/users/register/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupReq)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerDoctor_createsDoctorAndReturns201() throws Exception {
        String email = "doctor+" + UUID.randomUUID() + "@example.com";

        DoctorRegisterRequest req = new DoctorRegisterRequest(
                "Integration Doctor",
                email,
                "Doctor@pass12",
                com.sociolab.surehealth.enums.Speciality.CARDIOLOGY,
                "LIC-INT-123",
                5
        );

        mockMvc.perform(post("/api/v1/users/register/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.name").value("Integration Doctor"))
                .andExpect(jsonPath("$.data.role").value("PENDING_DOCTOR"));

        Optional<User> saved = userRepository.findByEmail(email);
        assertTrue(saved.isPresent());
        assertThat(saved.get().getRole().name()).isEqualTo("PENDING_DOCTOR");
    }



    @Test
    void registerPatient_invalidEmail_returnsBadRequest() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest(
                "Bad Email",
                "not-an-email",
                "Strongp@ssword"
        );

        mockMvc.perform(post("/api/v1/users/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerPatient_shortPassword_returnsBadRequest() throws Exception {
        UserRegisterRequest req = new UserRegisterRequest(
                "Short Pass",
                "shortpass+" + UUID.randomUUID() + "@example.com",
                "short"
        );

        mockMvc.perform(post("/api/v1/users/register/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }


}
