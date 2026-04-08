package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.OpinionRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpinionControllerIT extends TestContainersConfig {

    private static final String BASE = "/api/v1/opinions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private MedicalCaseRepository caseRepository;

    @Autowired
    private OpinionRepository opinionRepository;

    private User doctor;
    private User patient;

    @BeforeEach
    void setup() {
        doctor = testDataFactory.createDoctor("Doc2tor!pass").getUser();
        patient = testDataFactory.createPatient("Pati3nt!pass");
    }

    private UserPrincipal mockDoctor() {
        return new UserPrincipal(doctor.getId(), doctor.getEmail(), Role.DOCTOR, "");
    }

    private RequestPostProcessor doctorAuth() {
        var principal = mockDoctor();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    @Nested
    @DisplayName("SubmitOpinion")
    class SubmitOpinionTests {

        @Test
        @DisplayName("shouldSubmitOpinionSuccessfully")
        void shouldSubmitOpinionSuccessfully() throws Exception {
            MedicalCase mc = new MedicalCase();
            mc.setTitle("Case");
            mc.setDescription("Desc");
            mc.setPatient(patient);
            mc.setDoctor(doctor);
            mc.setStatus(CaseStatus.ACCEPTED);
            mc.setCreatedAt(LocalDateTime.now());
            MedicalCase saved = caseRepository.save(mc);

            OpinionRequest req = new OpinionRequest();
            req.setComment("Looks good");

            mockMvc.perform(post(BASE + "/{caseId}", saved.getId()).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.caseId").value(saved.getId().intValue()))
                    .andExpect(jsonPath("$.data.opinionText").value("Looks good"));

            MedicalCase updated = caseRepository.findById(saved.getId()).orElseThrow();
            assertEquals(CaseStatus.REVIEWED, updated.getStatus());
            assertEquals(1, opinionRepository.findAll().size());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            OpinionRequest req = new OpinionRequest();
            req.setComment("Looks good");

            mockMvc.perform(post(BASE + "/{caseId}", 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenValidationFails")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            mockMvc.perform(post(BASE + "/{caseId}", 1L).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"comment\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenCaseMissing")
        void shouldReturnNotFoundWhenCaseMissing() throws Exception {
            OpinionRequest req = new OpinionRequest();
            req.setComment("Looks good");

            mockMvc.perform(post(BASE + "/{caseId}", 999999L).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenCaseStatusInvalid")
        void shouldReturnBadRequestWhenCaseStatusInvalid() throws Exception {
            MedicalCase mc = new MedicalCase();
            mc.setTitle("Case");
            mc.setDescription("Desc");
            mc.setPatient(patient);
            mc.setDoctor(doctor);
            mc.setStatus(CaseStatus.SUBMITTED);
            mc.setCreatedAt(LocalDateTime.now());
            MedicalCase saved = caseRepository.save(mc);

            OpinionRequest req = new OpinionRequest();
            req.setComment("Looks good");

            mockMvc.perform(post(BASE + "/{caseId}", saved.getId()).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMalformedJson")
        void shouldReturnBadRequestWhenMalformedJson() throws Exception {
            mockMvc.perform(post(BASE + "/{caseId}", 1L).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());
        }
    }
}
