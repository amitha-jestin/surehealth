package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.enums.CaseStatus;
import org.springframework.data.domain.Page;

public interface CaseService {

    CaseResponse submitCase(Long patientId, CaseRequest request);

    Page<CaseResponse> getMyCases(Long userId, int page, int size);

    CaseResponse updateCaseStatus(Long caseId, Long userId, CaseStatus status);
}
