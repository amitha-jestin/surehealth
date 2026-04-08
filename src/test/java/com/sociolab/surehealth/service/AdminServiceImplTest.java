package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorResponse;
import com.sociolab.surehealth.dto.PatientSummary;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.Doctor;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DoctorRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    @DisplayName("shouldApproveDoctor_whenPending")
    void shouldApproveDoctor_whenPending() {
        User user = buildUser(2L, AccountStatus.PENDING, Role.PENDING_DOCTOR);
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setUserId(2L);

        when(doctorRepository.findByUserId(2L)).thenReturn(Optional.of(doctor));
        when(doctorRepository.save(any(Doctor.class))).thenReturn(doctor);

        adminService.approveDoctor(1L, 2L);

        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        assertEquals(Role.DOCTOR, user.getRole());
        verify(doctorRepository).save(doctor);
    }

    @Test
    @DisplayName("shouldThrowUserActive_whenApprovingActiveDoctor")
    void shouldThrowUserActive_whenApprovingActiveDoctor() {
        User user = buildUser(2L, AccountStatus.ACTIVE, Role.DOCTOR);
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setUserId(2L);

        when(doctorRepository.findByUserId(2L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> adminService.approveDoctor(1L, 2L));
        assertEquals(ErrorType.USER_ACTIVE, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowUserBlocked_whenApprovingBlockedDoctor")
    void shouldThrowUserBlocked_whenApprovingBlockedDoctor() {
        User user = buildUser(2L, AccountStatus.BLOCKED, Role.PENDING_DOCTOR);
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setUserId(2L);

        when(doctorRepository.findByUserId(2L)).thenReturn(Optional.of(doctor));

        AppException ex = assertThrows(AppException.class, () -> adminService.approveDoctor(1L, 2L));
        assertEquals(ErrorType.USER_BLOCKED, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowInvalidOperation_whenAdminBlocksSelf")
    void shouldThrowInvalidOperation_whenAdminBlocksSelf() {
        AppException ex = assertThrows(AppException.class, () -> adminService.updateUserStatus(5L, 5L, AccountStatus.BLOCKED));
        assertEquals(ErrorType.INVALID_OPERATION, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldBlockUser_whenActive")
    void shouldBlockUser_whenActive() {
        User user = buildUser(2L, AccountStatus.ACTIVE, Role.PATIENT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        adminService.updateUserStatus(1L, 2L, AccountStatus.BLOCKED);

        assertEquals(AccountStatus.BLOCKED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("shouldThrowUserBlocked_whenBlockingAlreadyBlockedUser")
    void shouldThrowUserBlocked_whenBlockingAlreadyBlockedUser() {
        User user = buildUser(2L, AccountStatus.BLOCKED, Role.PATIENT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        AppException ex = assertThrows(AppException.class, () -> adminService.updateUserStatus(1L, 2L, AccountStatus.BLOCKED));
        assertEquals(ErrorType.INVALID_OPERATION, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldUnblockUser_whenBlocked")
    void shouldUnblockUser_whenBlocked() {
        User user = buildUser(2L, AccountStatus.BLOCKED, Role.PATIENT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        adminService.updateUserStatus(1L, 2L, AccountStatus.ACTIVE);

        assertEquals(AccountStatus.ACTIVE, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("shouldThrowUserActive_whenUnblockingActiveUser")
    void shouldThrowUserActive_whenUnblockingActiveUser() {
        User user = buildUser(2L, AccountStatus.ACTIVE, Role.PATIENT);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        AppException ex = assertThrows(AppException.class, () -> adminService.updateUserStatus(1L, 2L , AccountStatus.ACTIVE));
        assertEquals(ErrorType.INVALID_OPERATION, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowUserInvalidStatus_whenUnblockingNonBlockedUser")
    void shouldThrowUserInvalidStatus_whenUnblockingNonBlockedUser() {
        User user = buildUser(2L, AccountStatus.PENDING, Role.PENDING_DOCTOR);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        AppException ex = assertThrows(AppException.class, () -> adminService.updateUserStatus(1L, 2L, AccountStatus.ACTIVE));
        assertEquals(ErrorType.USER_INVALID_STATUS, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldReturnPatientsPage_whenGetAllPatients")
    void shouldReturnPatientsPage_whenGetAllPatients() {
        User patient = buildUser(1L, AccountStatus.ACTIVE, Role.PATIENT);
        patient.setName("Pat");
        patient.setEmail("pat@example.com");
        Page<User> page = new PageImpl<>(List.of(patient));

        when(userRepository.findByRole(eq(Role.PATIENT), any(Pageable.class))).thenReturn(page);

        Page<PatientSummary> result = adminService.getAllPatients(0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(patient.getId(), result.getContent().get(0).id());
    }

    @Test
    @DisplayName("shouldReturnDoctorsPage_whenGetAllDoctors")
    void shouldReturnDoctorsPage_whenGetAllDoctors() {
        User docUser = buildUser(2L, AccountStatus.ACTIVE, Role.DOCTOR);
        docUser.setEmail("doc@example.com");
        Doctor doctor = new Doctor();
        doctor.setUser(docUser);
        doctor.setUserId(2L);
        Page<Doctor> page = new PageImpl<>(List.of(doctor));

        when(doctorRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<DoctorResponse> result = adminService.getAllDoctors(0, 10);

        assertEquals(1, result.getTotalElements());
        assertEquals(2L, result.getContent().get(0).id());
    }

    private User buildUser(Long id, AccountStatus status, Role role) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        user.setRole(role);
        return user;
    }
}
