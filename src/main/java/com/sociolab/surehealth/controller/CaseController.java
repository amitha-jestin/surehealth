package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.CaseRequest;
import com.sociolab.surehealth.dto.CaseResponse;
import com.sociolab.surehealth.dto.DocumentResponse;
import com.sociolab.surehealth.service.CaseService;
import com.sociolab.surehealth.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final DocumentService documentService;

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

    @PostMapping("/{caseId}/documents")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<DocumentResponse>> uploadDocument(
            @PathVariable Long caseId,
            @RequestParam("files")List<MultipartFile> files,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(
                documentService.uploadDocument(caseId, email, files)
        );
    }

    @GetMapping("/{caseId}/documents")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<List<DocumentResponse>> getCaseDocuments(
            @PathVariable Long caseId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(
                documentService.getDocumentsForCase(caseId, email)
        );
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


    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<List<CaseResponse>> getMyCases(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(caseService.getMyCases(email));
    }
}



