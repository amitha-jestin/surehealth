package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.service.CaseService;
import com.sociolab.surehealth.service.DocumentService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
public class CaseController {

    private final CaseService caseService;
    private final DocumentService documentService;

    // ================= SUBMIT CASE =================
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<CaseResponse>> submitCase(
            @Valid @RequestBody CaseRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        CaseResponse response = caseService.submitCase(email, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(response));
    }

    // ================= UPLOAD DOCUMENT =================
    @PostMapping("/{caseId}/documents")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PagedResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long caseId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Page<DocumentResponse> response =
                documentService.uploadDocuments(caseId, email, files);
        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

    // ================= GET CASE DOCUMENTS =================
    @GetMapping("/{caseId}/documents")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<PagedResponse<DocumentResponse>> getCaseDocuments(
            @PathVariable Long caseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication
    ) {
        String email = authentication.getName();
        Page<DocumentResponse> response =
                documentService.getDocumentsForCase(caseId, email, page, size);
        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

    // ================= ACCEPT CASE =================
    @PatchMapping("/{caseId}/accept")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<CaseResponse>> acceptCase(
            @PathVariable Long caseId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        CaseResponse response = caseService.acceptCase(caseId, email);
        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    // ================= REJECT CASE =================
    @PatchMapping("/{caseId}/reject")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<CaseResponse>> rejectCase(
            @PathVariable Long caseId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        CaseResponse response = caseService.rejectCase(caseId, email);
        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    // ================= GET MY CASES =================
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<PagedResponse<CaseResponse>> getMyCases(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        String email = authentication.getName();
        Page<CaseResponse> pagedCases =
                caseService.getMyCases(email, page, size);
        return ResponseEntity.ok(ResponseUtil.paged(pagedCases));
    }
}