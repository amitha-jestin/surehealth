package com.sociolab.surehealth.dto;

import com.sociolab.surehealth.enums.CaseStatus;
import com.sociolab.surehealth.enums.Speciality;
import com.sociolab.surehealth.enums.Urgency;

import java.time.LocalDateTime;

public record CaseResponse(Long caseId,
                           String title,
                           String description,
                           Speciality speciality,
                           Urgency urgency,
                           CaseStatus status,
                           LocalDateTime createdAt,
                            Long doctorId
) {
}
