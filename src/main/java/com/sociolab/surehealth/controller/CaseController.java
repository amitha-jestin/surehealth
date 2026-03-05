package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.service.CaseService;
import com.sociolab.surehealth.service.DocumentService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.List;

@RestController
@Validated
@RequestMapping(value = "/api/v1/cases", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
public class CaseController {

    private final CaseService caseService;
    private final DocumentService documentService;

    // ================= SUBMIT CASE =================
    @Operation(summary = "Submit a new case", description = "Patient submits a new medical case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Case submitted successfully"),
            @ApiResponse(responseCode = "404", description = "Patient or doctor not found"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid data")
    })
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<CaseResponse>> submitCase(
            @Valid @RequestBody CaseRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.info("CASE_SUBMIT_ATTEMPT: patient={} traceId={}", email, MDC.get("traceId"));

        CaseResponse response = caseService.submitCase(email, request);

        log.info("CASE_SUBMIT_SUCCESS: patient={} caseId={} traceId={}",
                email, response.caseId(), MDC.get("traceId"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(response));
    }

    // ================= UPLOAD DOCUMENT =================
    @Operation(summary = "Upload documents for a case", description = "Patient uploads documents for their case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents uploaded successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @PostMapping("/{caseId}/documents")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PagedResponse<DocumentResponse>> uploadDocument(
            @PathVariable Long caseId,
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.info("DOCUMENT_UPLOAD_ATTEMPT: patient={} caseId={} fileCount={} traceId={}",
                email, caseId, files.size(), MDC.get("traceId"));

        Page<DocumentResponse> response = documentService.uploadDocuments(caseId, email, files);

        log.info("DOCUMENT_UPLOAD_SUCCESS: patient={} caseId={} uploadedCount={} traceId={}",
                email, caseId, response.getNumberOfElements(), MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

    // ================= GET CASE DOCUMENTS =================
    @Operation(summary = "Get documents for a case", description = "Retrieve documents for a case (patient or doctor)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Documents retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @GetMapping("/{caseId}/documents")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<PagedResponse<DocumentResponse>> getCaseDocuments(
            @PathVariable Long caseId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.debug("CASE_DOC_QUERY: user={} caseId={} page={} size={} traceId={}",
                email, caseId, page, size, MDC.get("traceId"));

        Page<DocumentResponse> response = documentService.getDocumentsForCase(caseId, email, page, size);

        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

    // ================= ACCEPT CASE =================
    @Operation(summary = "Accept a case", description = "Doctor accepts an assigned case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Case accepted successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @PatchMapping("/{caseId}/accept")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<CaseResponse>> acceptCase(
            @PathVariable Long caseId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.info("CASE_ACCEPT_ATTEMPT: doctor={} caseId={} traceId={}", email, caseId, MDC.get("traceId"));

        CaseResponse response = caseService.acceptCase(caseId, email);

        log.info("CASE_ACCEPT_SUCCESS: doctor={} caseId={} traceId={}", email, caseId, MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    // ================= REJECT CASE =================
    @Operation(summary = "Reject a case", description = "Doctor rejects an assigned case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Case rejected successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @PatchMapping("/{caseId}/reject")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<CaseResponse>> rejectCase(
            @PathVariable Long caseId,
            Authentication authentication
    ) {
        String email = authentication.getName();
        log.info("CASE_REJECT_ATTEMPT: doctor={} caseId={} traceId={}", email, caseId, MDC.get("traceId"));

        CaseResponse response = caseService.rejectCase(caseId, email);

        log.info("CASE_REJECT_SUCCESS: doctor={} caseId={} traceId={}", email, caseId, MDC.get("traceId"));

        return ResponseEntity.ok(ResponseUtil.success(response));
    }

    // ================= GET MY CASES =================
    @Operation(summary = "Get my cases", description = "Retrieve all cases for current user (patient or doctor)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cases retrieved successfully")
    })
    @GetMapping("/my")
    @PreAuthorize("hasRole('PATIENT') or hasRole('DOCTOR')")
    public ResponseEntity<PagedResponse<CaseResponse>> getMyCases(
            Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        String email = authentication.getName();
        log.debug("MY_CASES_QUERY: user={} page={} size={} traceId={}", email, page, size, MDC.get("traceId"));

        Page<CaseResponse> pagedCases = caseService.getMyCases(email, page, size);

        return ResponseEntity.ok(ResponseUtil.paged(pagedCases));
    }
}