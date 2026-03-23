package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DocumentResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    Page<DocumentResponse> uploadDocuments(Long caseId, String email, List<MultipartFile> files);

    Page<DocumentResponse> getDocumentsForCase(Long caseId, String email, int page, int size);
}
