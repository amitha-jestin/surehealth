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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ================= UPLOAD DOCUMENTS =================
    @Transactional
    public Page<DocumentResponse> uploadDocuments(Long caseId, String email, List<MultipartFile> files) {
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND, "Case not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found"));

        if (!medicalCase.getPatientId().equals(user.getId())) {
            throw new AppException(ErrorType.ACCESS_DENIED, "You are not allowed to upload documents for this case");
        }

        List<MedicalDocument> savedDocs = files.stream()
                .map(file -> saveFile(file, medicalCase))
                .collect(Collectors.toList());

        Pageable pageable = PageRequest.of(0, savedDocs.size()); // single page
        return new PageImpl<>(savedDocs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList()), pageable, savedDocs.size());
    }

    // ================= GET DOCUMENTS FOR CASE (PAGED) =================
    public Page<DocumentResponse> getDocumentsForCase(Long caseId, String email, int page, int size) {
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND, "Case not found"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found"));

        if (!medicalCase.getPatientId().equals(user.getId()) &&
                !medicalCase.getDoctorId().equals(user.getId())) {
            throw new AppException(ErrorType.ACCESS_DENIED, "You are not allowed to view documents for this case");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        Page<MedicalDocument> documentsPage = documentRepository.findByMedicalCaseId(caseId, pageable);

        return documentsPage.map(this::mapToResponse);
    }

    // ================= HELPER: SAVE SINGLE FILE =================
    private MedicalDocument saveFile(MultipartFile file, MedicalCase medicalCase) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, fileName);

        try {
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, file.getBytes());
        } catch (IOException e) {
            throw new AppException(ErrorType.DOCUMENT_UPLOAD_FAILED, e.getMessage());
        }

        MedicalDocument doc = new MedicalDocument();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setFilePath(filePath.toString());
        doc.setMedicalCase(medicalCase);

        return documentRepository.save(doc);
    }

    // ================= HELPER: MAP TO RESPONSE =================
    private DocumentResponse mapToResponse(MedicalDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getFileType(),
                doc.getFilePath(),
                doc.getMedicalCase().getId(),
                doc.getUploadedAt()
        );
    }
}