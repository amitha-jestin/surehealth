package com.sociolab.surehealth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.enums.Urgency;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.MedicalDocument;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DocumentRepository;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.OutboxEventRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CaseControllerIT extends TestContainersConfig {

    private static final String BASE = "/api/v1/cases";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataFactory testDataFactory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicalCaseRepository caseRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private OutboxEventRepository outboxRepository;

    private User patient;
    private User doctor;

    @BeforeEach
    void setup() {
        patient = testDataFactory.createPatient("Pati3nt!pass");
        doctor = testDataFactory.createDoctor("Doc2tor!pass").getUser();
        doctor.setStatus(AccountStatus.ACTIVE);
        doctor.setRole(Role.DOCTOR);
        userRepository.save(doctor);
    }

    private UserPrincipal mockPatient() {
        return new UserPrincipal(patient.getId(), patient.getEmail(), Role.PATIENT, "");
    }

    private UserPrincipal mockDoctor() {
        return new UserPrincipal(doctor.getId(), doctor.getEmail(), Role.DOCTOR, "");
    }

    private RequestPostProcessor patientAuth() {
        var principal = mockPatient();
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
        );
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
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

    private MedicalCase createCase(CaseStatus status) {
        MedicalCase mc = new MedicalCase();
        mc.setTitle("Case");
        mc.setDescription("Desc");
        mc.setPatient(patient);
        mc.setDoctor(doctor);
        mc.setStatus(status);
        mc.setSpeciality(Speciality.CARDIOLOGY);
        mc.setUrgency(Urgency.HIGH);
        mc.setCreatedAt(LocalDateTime.now());
        return caseRepository.save(mc);
    }

    @Nested
    @DisplayName("SubmitCase")
    class SubmitCaseTests {

        @Test
        @DisplayName("shouldSubmitCaseSuccessfully")
        void shouldSubmitCaseSuccessfully() throws Exception {
            CaseRequest req = new CaseRequest(
                    "Chest pain",
                    "Details",
                    Speciality.CARDIOLOGY,
                    Urgency.HIGH,
                    doctor.getId()
            );
            long before_case = caseRepository.count();
            long before_outbox = outboxRepository.count();

            mockMvc.perform(post(BASE).with(patientAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.title").value("Chest pain"));

            assertEquals(before_case+1, caseRepository.findAll().size());
            assertEquals(before_outbox+1, outboxRepository.findAll().size());

        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            CaseRequest req = new CaseRequest(
                    "Chest pain",
                    "Details",
                    Speciality.CARDIOLOGY,
                    Urgency.HIGH,
                    doctor.getId()
            );

            mockMvc.perform(post(BASE )
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenValidationFails")
        void shouldReturnBadRequestWhenValidationFails() throws Exception {
            mockMvc.perform(post(BASE).with(patientAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"\",\"description\":\"\",\"doctorId\":1}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenDoctorMissing")
        void shouldReturnNotFoundWhenDoctorMissing() throws Exception {
            CaseRequest req = new CaseRequest(
                    "Chest pain",
                    "Details",
                    Speciality.CARDIOLOGY,
                    Urgency.HIGH,
                    999999L
            );

            mockMvc.perform(post(BASE).with(patientAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidEnum")
        void shouldReturnBadRequestWhenInvalidEnum() throws Exception {
            mockMvc.perform(post(BASE).with(patientAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"t\",\"description\":\"d\",\"speciality\":\"INVALID\",\"urgency\":\"HIGH\",\"doctorId\":1}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("UploadDocuments")
    class UploadDocumentsTests {

        @Test
        @DisplayName("shouldUploadDocumentsSuccessfully")
        void shouldUploadDocumentsSuccessfully() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);
            MockMultipartFile file = new MockMultipartFile(
                    "files", "report.pdf", "application/pdf", "report".getBytes()
            );
            Long before_count = documentRepository.count();

            mockMvc.perform(multipart(BASE + "/{caseId}/documents", mc.getId())
                            .file(file)
                            .with(patientAuth()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());

            assertEquals(before_count+1, documentRepository.findAll().size());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);
            MockMultipartFile file = new MockMultipartFile(
                    "files", "report.pdf", "application/pdf", "report".getBytes()
            );

            mockMvc.perform(multipart(BASE + "/{caseId}/documents", mc.getId())
                            .file(file))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMissingFiles")
        void shouldReturnBadRequestWhenMissingFiles() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(multipart(BASE + "/{caseId}/documents", mc.getId())
                            .with(patientAuth()))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenCaseMissing")
        void shouldReturnNotFoundWhenCaseMissing() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "report.txt", "text/plain", "report".getBytes()
            );

            mockMvc.perform(multipart(BASE + "/{caseId}/documents", 999999L)
                            .file(file)
                            .with(patientAuth()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenTooManyFiles")
        void shouldReturnBadRequestWhenTooManyFiles() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);
            MockMultipartFile file = new MockMultipartFile(
                    "files", "r.txt", "text/plain", "r".getBytes()
            );

            mockMvc.perform(multipart(BASE + "/{caseId}/documents", mc.getId())
                            .file(file).file(file).file(file).file(file).file(file).file(file)
                            .with(patientAuth()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GetCaseDocuments")
    class GetCaseDocumentsTests {

        @Test
        @DisplayName("shouldGetCaseDocumentsSuccessfully")
        void shouldGetCaseDocumentsSuccessfully() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);
            MedicalDocument doc = new MedicalDocument();
            doc.setFileName("a.txt");
            doc.setFileType("text/plain");
            doc.setFilePath("/tmp/a.txt");
            doc.setMedicalCase(mc);
            documentRepository.save(doc);

            mockMvc.perform(get(BASE + "/{caseId}/documents", mc.getId()).with(patientAuth())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(get(BASE + "/{caseId}/documents", mc.getId())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidPage")
        void shouldReturnBadRequestWhenInvalidPage() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(get(BASE + "/{caseId}/documents", mc.getId()).with(patientAuth())
                            .param("page", "-1")
                            .param("size", "10"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenCaseMissing")
        void shouldReturnNotFoundWhenCaseMissing() throws Exception {
            mockMvc.perform(get(BASE + "/{caseId}/documents", 999999L).with(patientAuth())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidSize")
        void shouldReturnBadRequestWhenInvalidSize() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(get(BASE + "/{caseId}/documents", mc.getId()).with(patientAuth())
                            .param("page", "0")
                            .param("size", "101"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("UpdateCaseStatus")
    class AcceptCaseTests {

        @Test
        @DisplayName("shouldUpdateCaseSuccessfully")
        void shouldAcceptCaseSuccessfully() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(patch(BASE + "/{caseId}/status", mc.getId()).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACCEPTED\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

            MedicalCase updated = caseRepository.findById(mc.getId()).orElseThrow();
            assertEquals(CaseStatus.ACCEPTED, updated.getStatus());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(patch(BASE + "/{caseId}/status", mc.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACCEPTED\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnNotFoundWhenCaseMissing")
        void shouldReturnNotFoundWhenCaseMissing() throws Exception {
            mockMvc.perform(patch(BASE + "/{caseId}/status", 999999L).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ACCEPTED\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidStatus")
        void shouldReturnBadRequestWhenInvalidStatus() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ACCEPTED);

            mockMvc.perform(patch(BASE + "/{caseId}/status", mc.getId()).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"REJECTED\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("UpdateCaseStatusEdgeCases")
    class RejectCaseTests {

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidEnum")
        void shouldReturnBadRequestWhenInvalidEnum() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(patch(BASE + "/{caseId}/status", mc.getId()).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"INVALID\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMissingBody")
        void shouldReturnBadRequestWhenMissingBody() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(patch(BASE + "/{caseId}/status", mc.getId()).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenMalformedJson")
        void shouldReturnBadRequestWhenMalformedJson() throws Exception {
            MedicalCase mc = createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(patch(BASE + "/{caseId}/status", mc.getId()).with(doctorAuth())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GetMyCases")
    class GetMyCasesTests {

        @Test
        @DisplayName("shouldGetMyCasesSuccessfully")
        void shouldGetMyCasesSuccessfully() throws Exception {
            createCase(CaseStatus.ASSIGNED);

            mockMvc.perform(get(BASE + "/my").with(patientAuth())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());
        }

        @Test
        @DisplayName("shouldReturnUnauthorizedWhenNoAuth")
        void shouldReturnUnauthorizedWhenNoAuth() throws Exception {
            mockMvc.perform(get(BASE + "/my")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("shouldReturnBadRequestWhenInvalidSize")
        void shouldReturnBadRequestWhenInvalidSize() throws Exception {
            mockMvc.perform(get(BASE + "/my").with(patientAuth())
                            .param("page", "0")
                            .param("size", "0"))
                    .andExpect(status().isBadRequest());
        }
    }
}
