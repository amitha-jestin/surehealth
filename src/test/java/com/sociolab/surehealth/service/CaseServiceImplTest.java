package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.enums.*;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseServiceImplTest {

    @Mock
    private MedicalCaseRepository caseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private CaseServiceImpl caseService;

    @Test
    @DisplayName("shouldSubmitCase_whenValid")
    void shouldSubmitCase_whenValid() {
        User patient = buildUser(1L, "patient@example.com", Role.PATIENT, AccountStatus.ACTIVE);
        User doctorUser = buildUser(2L, "doc@example.com", Role.DOCTOR, AccountStatus.ACTIVE);
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setUserId(2L);

        CaseRequest request = new CaseRequest("Title", "Desc", Speciality.CARDIOLOGY, Urgency.HIGH, 2L);

        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findByUserId(2L)).thenReturn(Optional.of(doctor));
        when(caseRepository.save(any(MedicalCase.class))).thenAnswer(invocation -> {
            MedicalCase mc = invocation.getArgument(0);
            mc.setId(100L);
            return mc;
        });

        CaseResponse response = caseService.submitCase(patient.getId(), request);

        assertEquals(100L, response.caseId());
        assertEquals("Title", response.title());
        assertEquals(CaseStatus.ASSIGNED, response.status());
        verify(outboxService).enqueue(eq("CASE_CREATED"), eq("MedicalCase"), eq("100"), any());
    }

    @Test
    @DisplayName("shouldThrowUserInvalidStatus_whenDoctorInactiveOnSubmit")
    void shouldThrowUserInvalidStatus_whenDoctorInactiveOnSubmit() {
        User patient = buildUser(1L, "patient@example.com", Role.PATIENT, AccountStatus.ACTIVE);
        User doctorUser = buildUser(2L, "doc@example.com", Role.DOCTOR, AccountStatus.BLOCKED);
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setUserId(2L);

        CaseRequest request = new CaseRequest("Title", "Desc", Speciality.CARDIOLOGY, Urgency.HIGH, 2L);

        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(doctorRepository.findByUserId(2L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> caseService.submitCase(patient.getId(), request));
        assertEquals(ErrorType.USER_INVALID_STATUS, ex.getErrorType());
        verify(caseRepository, never()).save(any(MedicalCase.class));
    }

    @Test
    @DisplayName("shouldReturnDoctorCases_whenRoleDoctor")
    void shouldReturnDoctorCases_whenRoleDoctor() {
        User doctor = buildUser(2L, "doc@example.com", Role.DOCTOR, AccountStatus.ACTIVE);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        MedicalCase mc = new MedicalCase();
        mc.setId(1L);
        mc.setTitle("Case");
        mc.setDoctor(doctor);
        mc.setStatus(CaseStatus.ASSIGNED);
        mc.setCreatedAt(LocalDateTime.now());

        Page<MedicalCase> page = new PageImpl<>(List.of(mc));
        when(caseRepository.findByDoctor_Id(eq(2L), any(Pageable.class))).thenReturn(page);

        Page<CaseResponse> result = caseService.getMyCases(doctor.getId(), 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(caseRepository).findByDoctor_Id(eq(2L), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldReturnPatientCases_whenRolePatient")
    void shouldReturnPatientCases_whenRolePatient() {
        User patient = buildUser(1L, "patient@example.com", Role.PATIENT, AccountStatus.ACTIVE);
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        MedicalCase mc = new MedicalCase();
        mc.setId(1L);
        mc.setTitle("Case");
        mc.setPatient(patient);
        mc.setStatus(CaseStatus.ASSIGNED);
        mc.setCreatedAt(LocalDateTime.now());

        Page<MedicalCase> page = new PageImpl<>(List.of(mc));
        when(caseRepository.findByPatient_Id(eq(1L), any(Pageable.class))).thenReturn(page);

        Page<CaseResponse> result = caseService.getMyCases(patient.getId(), 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(caseRepository).findByPatient_Id(eq(1L), any(Pageable.class));
    }

    @Test
    @DisplayName("shouldThrowAccessDenied_whenRoleNotAllowed")
    void shouldThrowAccessDenied_whenRoleNotAllowed() {
        User admin = buildUser(1L, "admin@example.com", Role.ADMIN, AccountStatus.ACTIVE);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        AppException ex = assertThrows(AppException.class, () -> caseService.getMyCases(admin.getId(), 0, 10));
        assertEquals(ErrorType.ACCESS_DENIED, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldAcceptCase_whenAssignedAndDoctorMatches")
    void shouldAcceptCase_whenAssignedAndDoctorMatches() {
        User doctorUser = buildUser(2L, "doc@example.com", Role.DOCTOR, AccountStatus.ACTIVE);
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setUserId(2L);

        MedicalCase mc = new MedicalCase();
        mc.setId(10L);
        mc.setDoctor(doctorUser);
        mc.setPatient(buildUser(1L, "patient@example.com", Role.PATIENT, AccountStatus.ACTIVE));
        mc.setStatus(CaseStatus.ASSIGNED);

        when(caseRepository.findById(10L)).thenReturn(Optional.of(mc));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        CaseResponse response = caseService.updateCaseStatus(10L, doctorUser.getId(), CaseStatus.ACCEPTED);

        assertEquals(CaseStatus.ACCEPTED, response.status());
    }

    @Test
    @DisplayName("shouldThrowAccessDenied_whenDoctorNotAssigned")
    void shouldThrowAccessDenied_whenDoctorNotAssigned() {
        User doctorUser = buildUser(3L, "doc@example.com", Role.DOCTOR, AccountStatus.ACTIVE);
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setUserId(3L);

        MedicalCase mc = new MedicalCase();
        mc.setId(10L);
        mc.setDoctor(buildUser(2L, "other@example.com", Role.DOCTOR, AccountStatus.ACTIVE));
        mc.setStatus(CaseStatus.ASSIGNED);

        when(caseRepository.findById(10L)).thenReturn(Optional.of(mc));
        when(doctorRepository.findById(3L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> caseService.updateCaseStatus(10L, doctorUser.getId(), CaseStatus.ACCEPTED));
        assertEquals(ErrorType.ACCESS_DENIED, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowInvalidOperation_whenRejectingNonAssignedCase")
    void shouldThrowInvalidOperation_whenRejectingNonAssignedCase() {
        User doctorUser = buildUser(2L, "doc@example.com", Role.DOCTOR, AccountStatus.ACTIVE);
        Doctor doctor = new Doctor();
        doctor.setUser(doctorUser);
        doctor.setUserId(2L);

        MedicalCase mc = new MedicalCase();
        mc.setId(10L);
        mc.setDoctor(doctorUser);
        mc.setStatus(CaseStatus.ACCEPTED);

        when(caseRepository.findById(10L)).thenReturn(Optional.of(mc));
        when(doctorRepository.findById(2L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> caseService.updateCaseStatus(10L, doctorUser.getId(), CaseStatus.REJECTED));
        assertEquals(ErrorType.INVALID_OPERATION, ex.getErrorType());
    }

    private User buildUser(Long id, String email, Role role, AccountStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        return user;
    }
}
