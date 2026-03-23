package com.sociolab.surehealth.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentValidator {

    void validate(MultipartFile file);
}
