package com.sociolab.surehealth.service;

/**
 * Simple holder for file storage metadata returned by FileStorageService.
 * Kept as a record so test code can construct it with positional args.
 */
public record StoredFile(String originalFileName, String contentType, String fileName, String filePath) {
}

