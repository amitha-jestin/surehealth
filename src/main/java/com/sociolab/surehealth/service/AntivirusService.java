package com.sociolab.surehealth.service;

import java.nio.file.Path;

/**
 * Pluggable antivirus/scanner interface for uploaded files.
 * Implementations should throw a runtime exception or app-specific exception
 * if the file is infected or scanning fails.
 */
public interface AntivirusService {
    /**
     * Scan the given file path. Throw an exception if infected or scanning failed.
     */
    void scan(Path filePath) throws Exception;
}

