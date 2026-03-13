package com.sociolab.surehealth.integration;

import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminIntegrationIT extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserRepository userRepository;

    private Doctor doctor;
    private User patient;
    private User admin;

    @BeforeEach
    void setup() {
        // create an admin and set SecurityContext as UserPrincipal with ADMIN role
        admin = testDataFactory.createAdmin("aA1@dminpass");

        var principal = new com.sociolab.surehealth.security.UserPrincipal(admin.getId(), admin.getEmail(), Role.ADMIN, "");
        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name())));
        SecurityContextHolder.getContext().setAuthentication(auth);

        // create test doctor and patient
        doctor = testDataFactory.createDoctor("Doc2tor!pass");
        patient = testDataFactory.createPatient("D1@atientpass");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approveDoctor_realIntegration() throws Exception {

        mockMvc.perform(patch("/api/v1/admin/doctors/{doctorId}/approve", doctor.getUser().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.message").value("Doctor approved successfully"));

        User savedDoctor = userRepository.findById(doctor.getUser().getId()).orElseThrow();

        assertEquals(AccountStatus.ACTIVE, savedDoctor.getStatus());

    }

    @Test
    void blockUser_realIntegration() throws Exception {

        mockMvc.perform(patch("/api/v1/admin/doctors/{doctorId}/approve", doctor.getUser().getId()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/block", doctor.getUser().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.message").value("User blocked successfully"));

        User savedDoctor = userRepository.findById(doctor.getUser().getId()).orElseThrow();

        assertEquals(AccountStatus.BLOCKED, savedDoctor.getStatus());
    }

    @Test
    void unblockUser_realIntegration() throws Exception {

        mockMvc.perform(patch("/api/v1/admin/doctors/{doctorId}/approve", doctor.getUser().getId()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/block", doctor.getUser().getId()))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/admin/users/{userId}/unblock", doctor.getUser().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.message").value("User unblocked successfully"));
        User savedDoctor = userRepository.findById(doctor.getUser().getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, savedDoctor.getStatus());
    }

    @Test
    void getAllPatients_realIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").exists());
    }

    @Test
    void getAllDoctors_realIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/doctors")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").exists());
    }
}
