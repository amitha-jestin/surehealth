package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIT extends TestContainersConfig {

    private static final String BASE = "/api/v1/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @BeforeEach
    void setup() {
    }

    @Nested
    @DisplayName("RegisterUser")
    class RegisterUserTests {

        @Test
        @DisplayName("shouldRegisterPatientSuccessfully")
        void shouldRegisterPatientSuccessfully() throws Exception {
            UserRegisterRequest req = new UserRegisterRequest(
                    "Patient User",
                    "patient+" + System.currentTimeMillis() + "@example.com",
                    "Passw01r@",
                    Role.PATIENT,
                    null,
                    null,
                    null
            );

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email").value(req.getEmail()))
                    .andExpect(jsonPath("$.data.role").value("PATIENT"));

            User saved = userRepository.findByEmail(req.getEmail()).orElseThrow();
            assertEquals(Role.PATIENT, saved.getRole());
        }

        @Test
        @DisplayName("shouldRegisterDoctorSuccessfully")
        void shouldRegisterDoctorSuccessfully() throws Exception {
            UserRegisterRequest req = new UserRegisterRequest(
                    "Doctor User",
                    "doctor+" + System.currentTimeMillis() + "@example.com",
                    "Passw0r1d@",
                    Role.DOCTOR,
                    Speciality.CARDIOLOGY,
                    "LIC-" + System.currentTimeMillis(),
                    5
            );

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.email").value(req.getEmail()))
                    .andExpect(jsonPath("$.data.role").value("PENDING_DOCTOR"));

            User saved = userRepository.findByEmail(req.getEmail()).orElseThrow();
            assertEquals(Role.PENDING_DOCTOR, saved.getRole());
            Doctor doctor = doctorRepository.findByUserId(saved.getId()).orElse(null);
            assertNotNull(doctor);
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenValidationFails")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"\",\"email\":\"\",\"password\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenDoctorFieldsMissing")
        void shouldReturnBadRequestWhenDoctorFieldsMissing() throws Exception {
            UserRegisterRequest req = new UserRegisterRequest(
                    "Doctor User",
                    "doctor+" + System.currentTimeMillis() + "@example.com",
                    "Passw0rd!",
                    Role.DOCTOR,
                    null,
                    null,
                    null
            );

            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidEnum")
        void shouldReturnBadRequestWhenInvalidEnum() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"X\",\"email\":\"x@example.com\",\"password\":\"Passw0rd!\",\"role\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMalformedJson")
        void shouldReturnBadRequestWhenMalformedJson() throws Exception {
            mockMvc.perform(post(BASE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());
        }
    }
}
