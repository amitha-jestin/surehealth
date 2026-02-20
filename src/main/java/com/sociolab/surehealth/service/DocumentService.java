package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.dto.DocumentResponse;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.model.MedicalDocument;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.DocumentRepository;
import com.sociolab.surehealth.repository.MedicalCaseRepository;
import com.sociolab.surehealth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final MedicalCaseRepository caseRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    @Value("${file.upload-dir}")
    private String uploadDir;
    @Transactional
    public List<DocumentResponse> uploadDocument(Long caseId, String email, List<MultipartFile> files) {
        MedicalCase medicalCase = caseRepository.findCaseById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!medicalCase.getPatientId().equals(user.getId())){
            throw new AccessDeniedException("You are not allowed to upload documents for this case");

        }

        List<DocumentResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get( uploadDir + fileName);

            try {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, file.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("File upload failed", e);
            }

            MedicalDocument doc = new MedicalDocument();
            doc.setFileName(file.getOriginalFilename());
            doc.setFileType(file.getContentType());
            doc.setFilePath(filePath.toString());
            doc.setMedicalCase(medicalCase);

            documentRepository.save(doc);

            responses.add(mapToResponse(doc));
        }

        return responses;

    }

    private DocumentResponse mapToResponse(MedicalDocument doc) {
        DocumentResponse res = new DocumentResponse(doc.getId(), doc.getFileName(), doc.getFileType(), doc.getFilePath(),doc.getMedicalCase().getId(),doc.getUploadedAt());
        return res;
    }


    public List<DocumentResponse> getDocumentsForCase(Long caseId, String email) {
        MedicalCase medicalCase = caseRepository.findCaseById(caseId)
                .orElseThrow(() -> new RuntimeException("Case not found"));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!medicalCase.getPatientId().equals(user.getId()) && !medicalCase.getDoctorId().equals(user.getId())){
            throw new AccessDeniedException("You are not allowed to view documents for this case");

        }

        List<MedicalDocument> documents = documentRepository.findByMedicalCaseId(caseId);
        List<DocumentResponse> responses = new ArrayList<>();
        for (MedicalDocument doc : documents) {
            responses.add(mapToResponse(doc));
        }
        return responses;
    }
}
