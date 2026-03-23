package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import org.springframework.data.domain.Page;

public interface CaseService {

    CaseResponse submitCase(String patientEmail, CaseRequest request);

    CaseResponse acceptCase(Long caseId, String doctorEmail);

    CaseResponse rejectCase(Long caseId, String doctorEmail);

    Page<CaseResponse> getMyCases(String email, int page, int size);
}
