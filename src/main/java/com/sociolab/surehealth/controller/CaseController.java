package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.service.CaseService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/cases")
public class CaseController {
    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping("/{patientId}")
    public MedicalCase submitCase(
            @PathVariable Long patientId,
            @RequestBody MedicalCase medicalCase) {
        return caseService.submitCase(patientId, medicalCase);
    }

}
