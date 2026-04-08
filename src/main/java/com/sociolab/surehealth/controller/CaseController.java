package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.security.UserPrincipal;
import com.sociolab.surehealth.service.CaseService;
import com.sociolab.surehealth.service.DocumentService;
import com.sociolab.surehealth.utils.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<BaseResponse<CaseResponse>> submitCase(
            @Valid @RequestBody CaseRequest request
    , @AuthenticationPrincipal UserPrincipal patientPrincipal) {

        Long userId = patientPrincipal.userId();
        log.info("action=case_submit status=START userId={}", userId);

        CaseResponse response = caseService.submitCase(userId, request);

        log.info("action=case_submit status=SUCCESS userId={} caseId={}",
                userId, response.caseId());

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseUtil.success(response));
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
            @RequestParam("files") @Size(max = 5) List<MultipartFile> files
    , @AuthenticationPrincipal UserPrincipal patientPrincipal) {
        Long userId = patientPrincipal.userId();
        log.info("action=case_document_upload status=START userId={} caseId={} fileCount={}",
                userId, caseId, files.size());

        Page<DocumentResponse> response = documentService.uploadDocuments(caseId, userId, files);

        log.info("action=case_document_upload status=SUCCESS userId={} caseId={} uploadedCount={}",
                userId, caseId, response.getNumberOfElements());

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
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    , @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.userId();
        log.info("action=case_documents_fetch status=START userId={} caseId={} page={} size={}",
                userId, caseId, page, size);

        Page<DocumentResponse> response = documentService.getDocumentsForCase(caseId, userId, page, size);

        log.info("action=case_documents_fetch status=SUCCESS userId={} caseId={} count={}",
                userId, caseId, response.getNumberOfElements());
        return ResponseEntity.ok(ResponseUtil.paged(response));
    }

    // ================= UPDATE CASE STATUS =================
    @Operation(summary = "Update case status", description = "Doctor updates the status of a case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Case status updated successfully"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Case not found")
    })
    @PatchMapping("/{caseId}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<BaseResponse<CaseResponse>> updateCaseStatus(
            @PathVariable Long caseId,
            @RequestBody @Valid CaseStatusUpdateRequest request
    , @AuthenticationPrincipal UserPrincipal doctorPrincipal) {

        Long userId = doctorPrincipal.userId();
        log.info("action=case_status_update status=START userId={} caseId={} newStatus={}",
                userId, caseId, request.status());

        CaseResponse response = caseService.updateCaseStatus(caseId, userId, request.status());
        log.info("action=case_status_update status=SUCCESS userId={} caseId={} newStatus={}",
                userId, caseId, request.status());
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
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    , @AuthenticationPrincipal UserPrincipal userPrincipal) {
        Long userId = userPrincipal.userId();
        log.info("action=case_my_fetch status=START userId={} page={} size={}", userId, page, size);

        Page<CaseResponse> pagedCases = caseService.getMyCases(userId, page, size);

        log.info("action=case_my_fetch status=SUCCESS userId={} count={}", userId, pagedCases.getNumberOfElements());
        return ResponseEntity.ok(ResponseUtil.paged(pagedCases));
    }
}
