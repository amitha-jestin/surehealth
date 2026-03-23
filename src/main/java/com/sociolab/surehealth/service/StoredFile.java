package com.sociolab.surehealth.service;

public record StoredFile(
        String originalFileName,
        String contentType,
        String storedFileName,
        String filePath
) {}
