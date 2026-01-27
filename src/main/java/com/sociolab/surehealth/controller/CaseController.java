package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.service.CaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;


@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;

    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<CaseResponse> submitCase(
            @Valid @RequestBody CaseRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(caseService.submitCase(email, request));
    }

    @PatchMapping("/{caseId}/accept")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<CaseResponse> acceptCase(
            @PathVariable Long caseId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(
                caseService.acceptCase(caseId, email)
        );
    }

    @PatchMapping("/{caseId}/reject")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<CaseResponse> rejectCase(
            @PathVariable Long caseId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(
                caseService.rejectCase(caseId, email)
        );
    }
}



