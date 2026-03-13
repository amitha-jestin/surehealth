package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.LoginRequest;
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
class CaseControllerIT extends TestContainersConfig {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserRepository userRepository;

    // Happy path: patient submits a case, doctor sees it and accepts it
    @Test
    void submitCase_thenDoctorAccepts_and_patientAndDoctorCanQuery() throws Exception {

        // create patient and doctor
        String patientPassword = "PatientPass1!";
        User patient = testDataFactory.createPatient(patientPassword);

        String doctorPassword = "DoctorPass1!";
        Doctor doctor = testDataFactory.createDoctor(doctorPassword);
        User doctorUser = doctor.getUser();
        // activate doctor so actions are allowed
        doctorUser.setStatus(com.sociolab.surehealth.enums.AccountStatus.ACTIVE);
        // persist the updated status so authentication/login sees ACTIVE status
        userRepository.save(doctorUser);

        // save activated doctor user via login flow: need to login, but TestDataFactory saved user already in DB
        // perform login for patient to get token
        LoginRequest loginPatient = new LoginRequest(patient.getEmail(), patientPassword);
        MvcResult loginPatientRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginPatient)))
                .andExpect(status().isOk())
                .andReturn();

        String patientToken = objectMapper.readTree(loginPatientRes.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // login doctor
        LoginRequest loginDoctor = new LoginRequest(doctorUser.getEmail(), doctorPassword);
        MvcResult loginDoctorRes = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDoctor)))
                .andExpect(status().isOk())
                .andReturn();

        String doctorToken = objectMapper.readTree(loginDoctorRes.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // submit case
        CaseRequest caseReq = new CaseRequest();
        caseReq.setTitle("Test Case Title");
        caseReq.setDescription("Some description");
        caseReq.setSpeciality(Speciality.CARDIOLOGY);
        caseReq.setUrgency(com.sociolab.surehealth.enums.Urgency.HIGH);
        caseReq.setDoctorId(doctorUser.getId());

        MvcResult submitRes = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.caseId").isNumber())
                .andReturn();

        JsonNode submitRoot = objectMapper.readTree(submitRes.getResponse().getContentAsString());
        Long caseId = submitRoot.path("data").path("caseId").asLong();

        // doctor fetches his cases (my)
        mockMvc.perform(get("/api/v1/cases/my")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].caseId").value(caseId.intValue()));

        // doctor accepts case
        mockMvc.perform(patch("/api/v1/cases/" + caseId + "/accept")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // patient fetches his cases and sees accepted status
        mockMvc.perform(get("/api/v1/cases/my")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("ACCEPTED"));
    }

    @Test
    void doctorRejectsCase_returnsRejected() throws Exception {
        String patientPassword = "P2Pass!";
        User patient = testDataFactory.createPatient(patientPassword);

        String doctorPassword = "D2Pass!";
        Doctor doctor = testDataFactory.createDoctor(doctorPassword);
        User doctorUser = doctor.getUser();
        doctorUser.setStatus(com.sociolab.surehealth.enums.AccountStatus.ACTIVE);
        userRepository.save(doctorUser);

        // login patient and doctor
        LoginRequest loginPatient = new LoginRequest(patient.getEmail(), patientPassword);
        String patientToken = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginPatient)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("token").asText();

        LoginRequest loginDoctor = new LoginRequest(doctorUser.getEmail(), doctorPassword);
        String doctorToken = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginDoctor)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data").path("token").asText();

        // submit case
        CaseRequest caseReq = new CaseRequest();
        caseReq.setTitle("Case to reject");
        caseReq.setDescription("Desc");
        caseReq.setSpeciality(Speciality.NEUROLOGY);
        caseReq.setUrgency(com.sociolab.surehealth.enums.Urgency.LOW);
        caseReq.setDoctorId(doctorUser.getId());

        JsonNode submitRoot = objectMapper.readTree(mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());

        Long caseId = submitRoot.path("data").path("caseId").asLong();

        // doctor rejects
        mockMvc.perform(patch("/api/v1/cases/" + caseId + "/reject")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }
}
