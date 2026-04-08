package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterResponse;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.enums.Speciality;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private UserServiceImpl userService;

   @Test
    @DisplayName("shouldRegisterPatient_whenValid")
    void shouldRegisterPatient_whenValid() {
        UserRegisterRequest req = new UserRegisterRequest("Pat", "pat@example.com", "Password1!", Role.PATIENT, null, null, null);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("enc");

        User saved = new User();
        saved.setId(1L);
        saved.setName(req.getName());
        saved.setEmail(req.getEmail());
        saved.setRole(req.getRole());
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserRegisterResponse response = userService.registerUser(req);

        assertEquals(1L, response.id());
        assertEquals(Role.PATIENT.name(), response.role());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("shouldThrowDuplicateResource_whenRegisterPatientDuplicate")
    void shouldThrowDuplicateResource_whenRegisterPatientDuplicate() {
        UserRegisterRequest req = new UserRegisterRequest("Pat", "pat@example.com", "Password1!", Role.PATIENT, null, null, null);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        AppException ex = assertThrows(AppException.class, () -> userService.registerUser(req));
        assertEquals(ErrorType.DUPLICATE_RESOURCE, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldRegisterDoctor_whenValid")
    void shouldRegisterDoctor_whenValid() {
        UserRegisterRequest req = new UserRegisterRequest("Doc", "doc@example.com", "Password1!", Role.DOCTOR,
                Speciality.CARDIOLOGY, "LIC123", 5);
        when(passwordEncoder.encode(req.getPassword())).thenReturn("enc");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setName(req.getName());
        savedUser.setEmail(req.getEmail());
        savedUser.setRole(Role.PENDING_DOCTOR);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserRegisterResponse response = userService.registerUser(req);

        assertEquals(2L, response.id());
        assertEquals(Role.PENDING_DOCTOR.name(), response.role());
        verify(userRepository).save(any(User.class));
        verify(doctorRepository).save(any(Doctor.class));
    }

    @Test
    @DisplayName("shouldThrowDuplicateResource_whenRegisterDoctorDuplicate")
    void shouldThrowDuplicateResource_whenRegisterDoctorDuplicate() {
        UserRegisterRequest req = new UserRegisterRequest("Doc", "doc@example.com", "Password1!",Role.DOCTOR,
                Speciality.CARDIOLOGY, "LIC123", 5);
        when(userRepository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        AppException ex = assertThrows(AppException.class, () -> userService.registerUser(req));
        assertEquals(ErrorType.DUPLICATE_RESOURCE, ex.getErrorType());
    }
}
