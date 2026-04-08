package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DocumentResponse;
import com.sociolab.surehealth.model.MedicalDocument;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(MedicalDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getFileName(),
                doc.getFileType(),
                doc.getFileName(),
                doc.getMedicalCase().getId(),
                doc.getUploadedAt()
        );
    }
}
