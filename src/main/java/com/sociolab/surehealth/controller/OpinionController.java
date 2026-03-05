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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opinions")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
@Slf4j
public class OpinionController {

    private final OpinionService opinionService;

    // ================= SUBMIT OPINION =================
    @PostMapping("/{caseId}")
    public ResponseEntity<ApiResponse<OpinionResponse>> submitOpinion(
            @PathVariable Long caseId,
            @RequestBody @Valid OpinionRequest opinionRequest,
            @AuthenticationPrincipal String doctorEmail
    ) {

        log.info("OPINION_SUBMIT_ATTEMPT: doctor={} caseId={} traceId={}",
                doctorEmail, caseId, MDC.get("traceId"));

        OpinionResponse response =
                opinionService.submitOpinion(caseId, doctorEmail, opinionRequest);

        log.info("OPINION_SUBMIT_SUCCESS: doctor={} caseId={} opinionId={} traceId={}",
                doctorEmail, caseId, response.id(), MDC.get("traceId"));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseUtil.success(response));
    }
}