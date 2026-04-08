package com.sociolab.surehealth.service;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Component
@Slf4j
public class DefaultDocumentValidator implements DocumentValidator {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    @Override
    public void validate(MultipartFile file) {
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

        log.debug("action=document_validate status=SUCCESS name={} size={} type={}",
                file.getOriginalFilename(), file.getSize(), contentType);
    }
}
