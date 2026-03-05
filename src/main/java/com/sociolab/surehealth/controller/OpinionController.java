package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.*;
import com.sociolab.surehealth.service.OpinionService;
import com.sociolab.surehealth.utils.ResponseUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping(value = "/api/v1/opinions", produces = "application/json")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
@Slf4j
public class OpinionController {

    private final OpinionService opinionService;

    // ================= SUBMIT OPINION =================
    @Operation(summary = "Submit opinion on a case", description = "Doctor submits an opinion for a specific medical case")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Opinion submitted successfully"),
            @ApiResponse(responseCode = "404", description = "Case or Doctor not found"),
            @ApiResponse(responseCode = "400", description = "Invalid case status or unauthorized doctor")
    })
    @PostMapping("/{caseId}")
    public ResponseEntity<com.sociolab.surehealth.dto.ApiResponse<OpinionResponse>> submitOpinion(
            @PathVariable Long caseId,
            @RequestBody @Valid OpinionRequest opinionRequest,
            Authentication authentication
    ) {
        String doctorEmail = authentication.getName();
        log.info("OPINION_SUBMIT_ATTEMPT: doctor={} caseId={} traceId={}", doctorEmail, caseId, MDC.get("traceId"));

        OpinionResponse response = opinionService.submitOpinion(caseId, doctorEmail, opinionRequest);

        log.info("OPINION_SUBMIT_SUCCESS: doctor={} caseId={} opinionId={} traceId={}",
                doctorEmail, caseId, response.id(), MDC.get("traceId"));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(response));
    }
}