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
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentValidator documentValidator;
    private final FileStorageService fileStorageService;
    private final DocumentMapper documentMapper;

    // ================= UPLOAD DOCUMENTS =================
    @Transactional
    @Override
    public Page<DocumentResponse> uploadDocuments(Long caseId, Long userId, List<MultipartFile> files) {
        log.debug("action=case_document_upload status=NOOP layer=service method=uploadDocuments caseId={} userId={} fileCount={}",
                caseId, userId, files.size());

        if(files.isEmpty()){
            throw new AppException(ErrorType.VALIDATION_ERROR, "No files uploaded");
        }
        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("action=case_document_upload status=FAILED caseId={} reason=CASE_NOT_FOUND", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Case not found");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("action=case_document_upload status=FAILED userId={} reason=USER_NOT_FOUND", userId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });

        if (!medicalCase.getPatient().getId().equals(user.getId())) {
            log.warn("action=case_document_upload status=FAILED caseId={} userId={} reason=ACCESS_DENIED",
                    caseId, user.getId());
            throw new AppException(ErrorType.ACCESS_DENIED,
                    "You are not allowed to upload documents for this case");
        }



        List<MedicalDocument> savedDocs = files.stream()
                .map(file -> storeDocument(file, medicalCase))
                .toList();

        log.info("action=case_document_upload status=SUCCESS caseId={} uploadedCount={} userId={}",
                caseId, savedDocs.size(), user.getId());

        Pageable pageable = PageRequest.of(0, savedDocs.size());
        return new PageImpl<>(savedDocs.stream()
                .map(documentMapper::toResponse)
                .toList(), pageable, savedDocs.size());
    }

    // ================= GET DOCUMENTS FOR CASE =================
    @Override
    public Page<DocumentResponse> getDocumentsForCase(Long caseId, Long userId, int page, int size) {

        MedicalCase medicalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> {
                    log.warn("action=case_documents_fetch status=FAILED caseId={} reason=CASE_NOT_FOUND", caseId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "Case not found");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("action=case_documents_fetch status=FAILED userId={} reason=USER_NOT_FOUND", userId);
                    return new AppException(ErrorType.RESOURCE_NOT_FOUND, "User not found");
                });

        if (!medicalCase.getPatient().getId().equals(user.getId()) &&
                !medicalCase.getDoctor().getId().equals(user.getId())) {

            log.warn("action=case_documents_fetch status=FAILED caseId={} userId={} reason=ACCESS_DENIED",
                    caseId, user.getId());

            throw new AppException(ErrorType.ACCESS_DENIED,
                    "You are not allowed to view documents for this case");
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());
        Page<MedicalDocument> documentsPage =
                documentRepository.findByMedicalCase_Id(caseId, pageable);

        log.info("action=case_documents_fetch status=SUCCESS caseId={} count={}",
                caseId, documentsPage.getTotalElements());

        return documentsPage.map(documentMapper::toResponse);
    }

    // ================= SAVE SINGLE FILE =================
    private MedicalDocument storeDocument(MultipartFile file, MedicalCase medicalCase) {
        documentValidator.validate(file);
        StoredFile storedFile = fileStorageService.store(file);
        registerRollbackCleanup(storedFile);

        MedicalDocument doc = new MedicalDocument();
        doc.setFileName(storedFile.originalFileName());
        doc.setFileType(storedFile.contentType());
        doc.setFilePath(storedFile.filePath());
        doc.setMedicalCase(medicalCase);

        MedicalDocument saved = documentRepository.save(doc);

        log.info("action=case_document_upload status=SUCCESS documentId={} caseId={}",
                saved.getId(), medicalCase.getId());

        return saved;
    }

    private void registerRollbackCleanup(StoredFile storedFile) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteStoredFileQuietly(storedFile);
                }
            }
        });
    }

    private void deleteStoredFileQuietly(StoredFile storedFile) {
        try {
            Files.deleteIfExists(Paths.get(storedFile.filePath()));
        } catch (IOException ignored) {
            log.warn("action=file_cleanup status=FAILED storedFileName={}", storedFile.fileName());
        }
    }
}
