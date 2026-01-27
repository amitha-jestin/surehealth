package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.dto.OpinionResponse;
import com.sociolab.surehealth.service.OpinionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opinions")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class OpinionController {

    private final OpinionService opinionService;

    @PostMapping("/{caseId}")
    public ResponseEntity<OpinionResponse> submitOpinion(
            @PathVariable Long caseId,
            @RequestBody @Valid OpinionRequest opinionRequest,
            @AuthenticationPrincipal String doctorEmail // from JWT
    ) {
        OpinionResponse response = opinionService.submitOpinion(caseId, doctorEmail, opinionRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

