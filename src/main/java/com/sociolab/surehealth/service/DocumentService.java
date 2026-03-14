package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DocumentResponse;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.logging.LogUtil;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.MedicalDocument;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DocumentRepository;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Set;
import java.nio.file.StandardCopyOption;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;

    // ================= UPLOAD DOCUMENTS =================
    @Transactional
    public Page<DocumentResponse> uploadDocuments(Long caseId, String email, List<MultipartFile> files) {

        if(files.isEmpty()){
            throw new AppException(ErrorType.RESOURCE_NOT_FOUND, "No files uploaded");
        }
        log.info("Document upload attempt caseId={} userEmail={} fileCount={}",
                caseId, LogUtil.maskEmail(email), files.size());

        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("Upload failed - case not found caseId={}", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Case not found");
                });

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Upload failed - user not found email={}", LogUtil.maskEmail(email));
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });

        if (!medicalCase.getPatient().getId().equals(user.getId())) {
            log.warn("Unauthorized document upload attempt caseId={} userId={}",
                    caseId, user.getId());
            throw new AppException(ErrorType.ACCESS_DENIED,
                    "You are not allowed to upload documents for this case");
        }



        List<MedicalDocument> savedDocs = files.stream()
                .map(file -> saveFile(file, medicalCase))
                .toList();

        log.info("Documents uploaded successfully caseId={} uploadedCount={} userId={}",
                caseId, savedDocs.size(), user.getId());

        Pageable pageable = PageRequest.of(0, savedDocs.size());
        return new PageImpl<>(savedDocs.stream()
                .map(this::mapToResponse)
                .toList(), pageable, savedDocs.size());
    }

    // ================= GET DOCUMENTS FOR CASE =================
    public Page<DocumentResponse> getDocumentsForCase(Long caseId, String email, int page, int size) {

        log.debug("Fetching documents caseId={} email={} page={} size={}",
                caseId, LogUtil.maskEmail(email), page, size);

        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("Document fetch failed - case not found caseId={}", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Case not found");
                });

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Document fetch failed - user not found email={}", LogUtil.maskEmail(email));
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });

        if (!medicalCase.getPatient().getId().equals(user.getId()) &&
                !medicalCase.getDoctor().getId().equals(user.getId())) {

            log.warn("Unauthorized document access caseId={} userId={}",
                    caseId, user.getId());

            throw new AppException(ErrorType.ACCESS_DENIED,
                    "You are not allowed to view documents for this case");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        Page<MedicalDocument> documentsPage =
                documentRepository.findByMedicalCase_Id(caseId, pageable);

        log.debug("Documents fetched caseId={} totalElements={}",
                caseId, documentsPage.getTotalElements());

        return documentsPage.map(this::mapToResponse);
    }

    // ================= SAVE SINGLE FILE =================
    private MedicalDocument saveFile(MultipartFile file, MedicalCase medicalCase) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorType.VALIDATION_ERROR, "File is empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new AppException(ErrorType.VALIDATION_ERROR, "File exceeds maximum allowed size");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new AppException(ErrorType.VALIDATION_ERROR, "Unsupported file type");
        }


        String safeOriginalName = sanitizeFileName(file.getOriginalFilename());
        String fileName = UUID.randomUUID() + "_" + safeOriginalName;
        Path filePath = Paths.get(uploadDir, fileName);

        try {
            Files.createDirectories(filePath.getParent());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.debug("File written to disk generatedFileName={}", fileName);

        } catch (IOException e) {

            log.error("File upload failed caseId={} fileName={} error={}",
                    medicalCase.getId(),
                    file.getOriginalFilename(),
                    e.getMessage());

            throw new AppException(ErrorType.DOCUMENT_UPLOAD_FAILED,
                    "Failed to upload document");
        }

        MedicalDocument doc = new MedicalDocument();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setFilePath(filePath.toString());
        doc.setMedicalCase(medicalCase);

        MedicalDocument saved = documentRepository.save(doc);

        log.info("Document metadata saved documentId={} caseId={}",
                saved.getId(), medicalCase.getId());

        return saved;
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "file";
        }
        String baseName = Paths.get(originalFileName).getFileName().toString();
        String cleaned = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "file" : cleaned;
    }

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

