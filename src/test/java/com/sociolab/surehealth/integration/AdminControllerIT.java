package com.sociolab.surehealth.integration;

import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.security.UserPrincipal;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminControllerIT extends TestContainersConfig {

    private static final String BASE = "/api/v1/admin";
    private static final String APPROVE_DOCTOR = BASE + "/doctors/{doctorId}/approve";
    private static final String UPDATE_STATUS = BASE + "/users/{userId}/status";
    private static final String GET_PATIENTS = BASE + "/patients";
    private static final String GET_DOCTORS = BASE + "/doctors";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserRepository userRepository;

    private User admin;
    private Doctor doctor;
    private User patient;

    @BeforeEach
    void setup() {
        admin = testDataFactory.createAdmin("aA1@dminpass");
        doctor = testDataFactory.createDoctor("Doc2tor!pass");
        patient = testDataFactory.createPatient("D1@atientpass");
    }

    private UserPrincipal mockAdmin() {
        return new UserPrincipal(admin.getId(), admin.getEmail(), Role.ADMIN, "");
    }

    private RequestPostProcessor adminAuth() {
        var principal = mockAdmin();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @Nested
    @DisplayName("ApproveDoctor")
    class ApproveDoctorTests {

        @Test
        @DisplayName("shouldApproveDoctorSuccessfully")
        void shouldApproveDoctorSuccessfully() throws Exception {
            mockMvc.perform(patch(APPROVE_DOCTOR, doctor.getUser().getId()).with(adminAuth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meta.message").value("Doctor approved successfully"));

            User savedDoctor = userRepository.findById(doctor.getUser().getId()).orElseThrow();
            assertEquals(AccountStatus.ACTIVE, savedDoctor.getStatus());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAdmin")
        void shouldReturnUnauthorizedWhenNoAdmin() throws Exception {
            mockMvc.perform(patch(APPROVE_DOCTOR, doctor.getUser().getId()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenDoctorMissing")
        void shouldReturnNotFoundWhenDoctorMissing() throws Exception {
            mockMvc.perform(patch(APPROVE_DOCTOR, 999999L).with(adminAuth()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenDoctorIdInvalid")
        void shouldReturnBadRequestWhenDoctorIdInvalid() throws Exception {
            mockMvc.perform(patch(APPROVE_DOCTOR, 0L).with(adminAuth()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("UpdateUserStatus")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("shouldUpdateUserStatusSuccessfully")
        void shouldUpdateUserStatusSuccessfully() throws Exception {
            mockMvc.perform(patch(UPDATE_STATUS, doctor.getUser().getId()).with(adminAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStatus\":\"BLOCKED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meta.message").value("User blocked successfully"));

            User saved = userRepository.findById(doctor.getUser().getId()).orElseThrow();
            assertEquals(AccountStatus.BLOCKED, saved.getStatus());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAdmin")
        void shouldReturnUnauthorizedWhenNoAdmin() throws Exception {
            mockMvc.perform(patch(UPDATE_STATUS, doctor.getUser().getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStatus\":\"BLOCKED\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMissingBody")
        void shouldReturnBadRequestWhenMissingBody() throws Exception {
            mockMvc.perform(patch(UPDATE_STATUS, doctor.getUser().getId()).with(adminAuth())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidEnum")
        void shouldReturnBadRequestWhenInvalidEnum() throws Exception {
            mockMvc.perform(patch(UPDATE_STATUS, doctor.getUser().getId()).with(adminAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStatus\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenUserMissing")
        void shouldReturnNotFoundWhenUserMissing() throws Exception {
            mockMvc.perform(patch(UPDATE_STATUS, 999999L).with(adminAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"newStatus\":\"BLOCKED\"}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GetPatients")
    class GetPatientsTests {

        @Test
        @DisplayName("shouldGetPatientsSuccessfully")
        void shouldGetPatientsSuccessfully() throws Exception {
            mockMvc.perform(get(GET_PATIENTS).with(adminAuth())
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meta").exists())
                    .andExpect(jsonPath("$.content").exists());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAdmin")
        void shouldReturnUnauthorizedWhenNoAdmin() throws Exception {
            mockMvc.perform(get(GET_PATIENTS)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidPage")
        void shouldReturnBadRequestWhenInvalidPage() throws Exception {
            mockMvc.perform(get(GET_PATIENTS).with(adminAuth())
                            .param("page", "-1")
                            .param("size", "20"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GetDoctors")
    class GetDoctorsTests {

        @Test
        @DisplayName("shouldGetDoctorsSuccessfully")
        void shouldGetDoctorsSuccessfully() throws Exception {
            mockMvc.perform(get(GET_DOCTORS).with(adminAuth())
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meta").exists())
                    .andExpect(jsonPath("$.content").exists());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAdmin")
        void shouldReturnUnauthorizedWhenNoAdmin() throws Exception {
            mockMvc.perform(get(GET_DOCTORS)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidSize")
        void shouldReturnBadRequestWhenInvalidSize() throws Exception {
            mockMvc.perform(get(GET_DOCTORS).with(adminAuth())
                            .param("page", "0")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());
        }
    }
}
