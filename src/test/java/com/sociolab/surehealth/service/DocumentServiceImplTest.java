package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DocumentResponse;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.MedicalDocument;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DocumentRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private MedicalCaseRepository caseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentValidator documentValidator;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private DocumentMapper documentMapper;

    @InjectMocks
    private DocumentServiceImpl documentService;

    @Test
    @DisplayName("shouldThrowResourceNotFound_whenFilesEmpty")
    void shouldThrowResourceNotFound_whenFilesEmpty() {
        AppException ex = assertThrows(AppException.class,
                () -> documentService.uploadDocuments(1L, 2L, List.of()));
        assertEquals(ErrorType.VALIDATION_ERROR, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowAccessDenied_whenUserNotCasePatient")
    void shouldThrowAccessDenied_whenUserNotCasePatient() {
        MedicalCase medicalCase = new MedicalCase();
        User patient = new User();
        patient.setId(1L);
        medicalCase.setPatient(patient);
        medicalCase.setId(100L);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        when(caseRepository.findById(100L)).thenReturn(Optional.of(medicalCase));
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));

        List<MultipartFile> files = List.of(new MockMultipartFile("f", "a.pdf", "application/pdf", "x".getBytes()));

        AppException ex = assertThrows(AppException.class,
                () -> documentService.uploadDocuments(100L, otherUser.getId(), files));

        assertEquals(ErrorType.ACCESS_DENIED, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldUploadDocuments_whenValid")
    void shouldUploadDocuments_whenValid() {
        MedicalCase medicalCase = new MedicalCase();
        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@example.com");
        medicalCase.setPatient(patient);
        medicalCase.setId(100L);

        when(caseRepository.findById(100L)).thenReturn(Optional.of(medicalCase));
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        MultipartFile file1 = new MockMultipartFile("f1", "a.pdf", "application/pdf", "a".getBytes());
        MultipartFile file2 = new MockMultipartFile("f2", "b.pdf", "application/pdf", "b".getBytes());
        List<MultipartFile> files = List.of(file1, file2);

        when(fileStorageService.store(file1)).thenReturn(new StoredFile("a.txt", "text/plain", "a.txt", "/tmp/a.txt"));
        when(fileStorageService.store(file2)).thenReturn(new StoredFile("b.txt", "text/plain", "b.txt", "/tmp/b.txt"));

        when(documentRepository.save(any(MedicalDocument.class))).thenAnswer(invocation -> {
            MedicalDocument doc = invocation.getArgument(0);
            doc.setId(doc.getFileName().equals("a.txt") ? 1L : 2L);
            doc.setUploadedAt(LocalDateTime.now());
            return doc;
        });

        when(documentMapper.toResponse(any(MedicalDocument.class))).thenAnswer(invocation -> {
            MedicalDocument doc = invocation.getArgument(0);
            return new DocumentResponse(doc.getId(), doc.getFileName(), doc.getFileType(), doc.getFilePath(),
                    doc.getMedicalCase().getId(), doc.getUploadedAt());
        });

        Page<DocumentResponse> response = documentService.uploadDocuments(100L, patient.getId(), files);

        assertEquals(2, response.getTotalElements());
        verify(documentValidator, times(2)).validate(any(MultipartFile.class));
        verify(fileStorageService, times(2)).store(any(MultipartFile.class));
        verify(documentRepository, times(2)).save(any(MedicalDocument.class));
    }

    @Test
    @DisplayName("shouldThrowAccessDenied_whenUserNotCaseParticipant")
    void shouldThrowAccessDenied_whenUserNotCaseParticipant() {
        MedicalCase medicalCase = new MedicalCase();
        User patient = new User();
        patient.setId(1L);
        medicalCase.setPatient(patient);
        User doctor = new User();
        doctor.setId(2L);
        medicalCase.setDoctor(doctor);
        medicalCase.setId(100L);

        User otherUser = new User();
        otherUser.setId(3L);
        otherUser.setEmail("other@example.com");

        when(caseRepository.findById(100L)).thenReturn(Optional.of(medicalCase));
        when(userRepository.findById(otherUser.getId())).thenReturn(Optional.of(otherUser));

        AppException ex = assertThrows(AppException.class,
                () -> documentService.getDocumentsForCase(100L, otherUser.getId(), 0, 10));

        assertEquals(ErrorType.ACCESS_DENIED, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldReturnDocuments_whenAuthorized")
    void shouldReturnDocuments_whenAuthorized() {
        MedicalCase medicalCase = new MedicalCase();
        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@example.com");
        medicalCase.setPatient(patient);
        User doctor = new User();
        doctor.setId(2L);
        medicalCase.setDoctor(doctor);
        medicalCase.setId(100L);

        when(caseRepository.findById(100L)).thenReturn(Optional.of(medicalCase));
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        MedicalDocument doc = new MedicalDocument();
        doc.setId(1L);
        doc.setFileName("a.pdf");
        doc.setFileType("application/pdf");
        doc.setFilePath("/tmp/a.pdf");
        doc.setMedicalCase(medicalCase);
        doc.setUploadedAt(LocalDateTime.now());

        Page<MedicalDocument> page = new PageImpl<>(List.of(doc));
        when(documentRepository.findByMedicalCase_Id(eq(100L), any(Pageable.class))).thenReturn(page);
        when(documentMapper.toResponse(doc)).thenReturn(new DocumentResponse(
                doc.getId(), doc.getFileName(), doc.getFileType(), doc.getFilePath(), 100L, doc.getUploadedAt()
        ));

        Page<DocumentResponse> result = documentService.getDocumentsForCase(100L, patient.getId(), 0, 10);

        assertEquals(1, result.getTotalElements());
        verify(documentRepository).findByMedicalCase_Id(eq(100L), any(Pageable.class));
    }
}
