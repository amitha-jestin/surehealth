package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.testdata.TestDataFactory;
import com.sociolab.surehealth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class OpinionControllerIT extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserRepository userRepository;

    @Test
    void submitOpinion_success_afterDoctorAcceptedCase() throws Exception {

        // create patient and doctor
        String patientPassword = "PatientPass@1";
        User patient = testDataFactory.createPatient(patientPassword);

        String doctorPassword = "DoctorPass@1";
        Doctor doctor = testDataFactory.createDoctor(doctorPassword);
        User doctorUser = doctor.getUser();
        doctorUser.setStatus(com.sociolab.surehealth.enums.AccountStatus.ACTIVE);
        userRepository.save(doctorUser);

        // login patient
        LoginRequest loginPatient = new LoginRequest(patient.getEmail(), patientPassword);
        MvcResult patientLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPatient)))
                .andExpect(status().isOk())
                .andReturn();

        String patientToken = objectMapper.readTree(patientLoginRes.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // login doctor
        LoginRequest loginDoctor = new LoginRequest(doctorUser.getEmail(), doctorPassword);
        MvcResult doctorLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDoctor)))
                .andExpect(status().isOk())
                .andReturn();

        String doctorToken = objectMapper.readTree(doctorLoginRes.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // patient submits a case assigned to the doctor
        CaseRequest caseReq = new CaseRequest();
        caseReq.setTitle("Opinion test case");
        caseReq.setDescription("Description");
        caseReq.setSpeciality(Speciality.CARDIOLOGY);
        caseReq.setUrgency(com.sociolab.surehealth.enums.Urgency.MEDIUM);
        caseReq.setDoctorId(doctorUser.getId());

        MvcResult submitRes = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode submitRoot = objectMapper.readTree(submitRes.getResponse().getContentAsString());
        Long caseId = submitRoot.path("data").path("caseId").asLong();

        // doctor accepts the case
        mockMvc.perform(patch("/api/v1/cases/" + caseId + "/accept")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());

        // doctor submits opinion
        OpinionRequest opinionReq = new OpinionRequest();
        opinionReq.setComment("This is my expert opinion");

        mockMvc.perform(post("/api/v1/opinions/" + caseId)
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(opinionReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.opinionText").value("This is my expert opinion"))
                .andExpect(jsonPath("$.data.doctorId").value(doctorUser.getId().intValue()));
    }

    @Test
    void submitOpinion_forbidden_forPatient() throws Exception {
        String patientPassword = "Patt2!";
        User patient = testDataFactory.createPatient(patientPassword);

        // login patient
        LoginRequest loginPatient = new LoginRequest(patient.getEmail(), patientPassword);
        MvcResult patientLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPatient)))
                .andExpect(status().isOk())
                .andReturn();

        String patientToken = objectMapper.readTree(patientLoginRes.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // attempt to submit opinion as patient (should be forbidden by security)
        OpinionRequest opinionReq = new OpinionRequest();
        opinionReq.setComment("Patient can't submit this");

        mockMvc.perform(post("/api/v1/opinions/9999")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(opinionReq)))
                .andExpect(status().isForbidden());
    }
}

