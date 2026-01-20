package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.model.MedicalCase;
import com.sociolab.surehealth.service.CaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/cases")
public class CaseController {
    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @PostMapping("/{patientId}")
    public MedicalCase submitCase(@Valid
            @PathVariable Long patientId,
            @RequestBody CaseRequest caseReq) {
        return caseService.submitCase(patientId, caseReq);
    }

}
