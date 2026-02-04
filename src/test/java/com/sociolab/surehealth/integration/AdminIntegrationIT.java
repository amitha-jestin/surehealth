package com.sociolab.surehealth.integration;

import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import com.sociolab.surehealth.testdata.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ser.impl.UnknownSerializer;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class AdminIntegrationIT extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserRepository userRepository;

    private Doctor doctor;
    private User patient ;
    @BeforeEach
    void setup() {
        doctor = testDataFactory.createDoctor("doctorpass");
        patient = testDataFactory.createPatient("patientpass");

    }
    
    @Test
    void approveDoctor_realIntegration() throws Exception {


        mockMvc.perform(patch("/api/v1/admin/doctors/{id}/approve", doctor.getUser().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        User savedDoctor = userRepository.findById(doctor.getUser().getId()).orElseThrow();

        assertEquals(AccountStatus.ACTIVE, savedDoctor.getStatus());

    }

    @Test
    void blockUser_realIntegration() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/user/{id}/block", doctor.getUser().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        User savedDoctor = userRepository.findById(doctor.getUser().getId()).orElseThrow();

        assertEquals(AccountStatus.BLOCKED, savedDoctor.getStatus());
    }

    @Test
    void unblockUser_realIntegration() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/user/{id}/unblock" , doctor.getUser().getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
        User savedDoctor = userRepository.findById(doctor.getUser().getId()).orElseThrow();
        assertEquals(AccountStatus.ACTIVE, savedDoctor.getStatus());
    }

    @Test
    void getAllPatients_realIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/patients")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }

    @Test
    void getAllDoctors_realIntegration() throws Exception {
        mockMvc.perform(get("/api/v1/admin/doctors")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
    }
}
