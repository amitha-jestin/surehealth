package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.dto.OpinionResponse;
import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.Opinion;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.OpinionRepository;
import com.sociolab.surehealth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpinionServiceImplTest {

    @Mock
    private OpinionRepository opinionRepository;
    @Mock
    private MedicalCaseRepository caseRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OpinionServiceImpl opinionService;

    @Test
    @DisplayName("shouldSubmitOpinion_whenCaseAcceptedAndDoctorAssigned")
    void shouldSubmitOpinion_whenCaseAcceptedAndDoctorAssigned() {
        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setId(10L);
        medicalCase.setStatus(CaseStatus.ACCEPTED);

        User doctor = new User();
        doctor.setId(2L);
        doctor.setEmail("doc@example.com");
        medicalCase.setDoctor(doctor);

        OpinionRequest request = new OpinionRequest();
        request.setComment("Looks good");

        Opinion savedOpinion = new Opinion();
        savedOpinion.setId(55L);
        savedOpinion.setComment(request.getComment());

        when(caseRepository.findById(10L)).thenReturn(Optional.of(medicalCase));
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(opinionRepository.save(any(Opinion.class))).thenReturn(savedOpinion);

        OpinionResponse response = opinionService.submitOpinion(10L, doctor.getId(), request);

        assertEquals(55L, response.id());
        assertEquals(doctor.getId(), response.doctorId());
        assertEquals(10L, response.caseId());
        assertEquals(request.getComment(), response.opinionText());
        verify(caseRepository).save(medicalCase);
        assertEquals(CaseStatus.REVIEWED, medicalCase.getStatus());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFound_whenCaseMissing")
    void shouldThrowResourceNotFound_whenCaseMissing() {
        when(caseRepository.findById(10L)).thenReturn(Optional.empty());

        OpinionRequest request = new OpinionRequest();
        request.setComment("test");

        AppException ex = assertThrows(AppException.class,
                () -> opinionService.submitOpinion(10L, 2L, request));
        assertEquals(ErrorType.RESOURCE_NOT_FOUND, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowInvalidOperation_whenCaseNotAccepted")
    void shouldThrowInvalidOperation_whenCaseNotAccepted() {
        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setId(10L);
        medicalCase.setStatus(CaseStatus.SUBMITTED);

        when(caseRepository.findById(10L)).thenReturn(Optional.of(medicalCase));

        OpinionRequest request = new OpinionRequest();
        request.setComment("test");

        AppException ex = assertThrows(AppException.class,
                () -> opinionService.submitOpinion(10L, 10L, request));
        assertEquals(ErrorType.INVALID_OPERATION, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFound_whenDoctorMissing")
    void shouldThrowResourceNotFound_whenDoctorMissing() {
        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setId(10L);
        medicalCase.setStatus(CaseStatus.ACCEPTED);

        when(caseRepository.findById(10L)).thenReturn(Optional.of(medicalCase));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        OpinionRequest request = new OpinionRequest();
        request.setComment("test");

        AppException ex = assertThrows(AppException.class,
                () -> opinionService.submitOpinion(10L, 2L, request));
        assertEquals(ErrorType.RESOURCE_NOT_FOUND, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowInvalidOperation_whenDoctorNotAssigned")
    void shouldThrowInvalidOperation_whenDoctorNotAssigned() {
        MedicalCase medicalCase = new MedicalCase();
        medicalCase.setId(10L);
        medicalCase.setStatus(CaseStatus.ACCEPTED);

        User assignedDoctor = new User();
        assignedDoctor.setId(1L);
        medicalCase.setDoctor(assignedDoctor);

        User otherDoctor = new User();
        otherDoctor.setId(2L);
        otherDoctor.setEmail("doc@example.com");

        when(caseRepository.findById(10L)).thenReturn(Optional.of(medicalCase));
        when(userRepository.findById(otherDoctor.getId())).thenReturn(Optional.of(otherDoctor));

        OpinionRequest request = new OpinionRequest();
        request.setComment("test");

        AppException ex = assertThrows(AppException.class,
                () -> opinionService.submitOpinion(10L, otherDoctor.getId(), request));
        assertEquals(ErrorType.INVALID_OPERATION, ex.getErrorType());
    }
}
