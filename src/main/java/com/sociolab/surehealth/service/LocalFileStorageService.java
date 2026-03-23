package com.sociolab.surehealth.service;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Component
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public StoredFile store(MultipartFile file) {
        String safeOriginalName = sanitizeFileName(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "_" + safeOriginalName;
        Path filePath = Paths.get(uploadDir, storedFileName);

        try {
            Files.createDirectories(filePath.getParent());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("File written to disk storedFileName={}", storedFileName);
        } catch (IOException e) {
            log.error("File upload failed fileName={} error={}", file.getOriginalFilename(), e.getMessage());
            throw new AppException(ErrorType.DOCUMENT_UPLOAD_FAILED, "Failed to upload document");
        }

        return new StoredFile(
                file.getOriginalFilename(),
                file.getContentType(),
                storedFileName,
                filePath.toString()
        );
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "file";
        }
        String baseName = Paths.get(originalFileName).getFileName().toString();
        String cleaned = baseName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return cleaned.isBlank() ? "file" : cleaned;
    }
}
