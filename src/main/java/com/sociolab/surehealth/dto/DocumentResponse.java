package com.sociolab.surehealth.dto;

import java.time.LocalDateTime;

public record DocumentResponse(
        Long id,
        String fileName,
        String fileType,
        String filePath,
        Long caseId,
        LocalDateTime uploadedAt
        ){

}
