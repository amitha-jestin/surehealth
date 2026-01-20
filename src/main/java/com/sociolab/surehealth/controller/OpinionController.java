package com.sociolab.surehealth.controller;

import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.model.Opinion;
import com.sociolab.surehealth.service.OpinionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/opinions")
public class OpinionController {

    private final OpinionService opinionService;
    public OpinionController(OpinionService opinionService) {
        this.opinionService = opinionService;
    }

    @PostMapping("/{caseId}/{doctorId}")
    public Opinion submitOpinion(
            @PathVariable Long caseId,
            @PathVariable Long doctorId,
            @RequestBody OpinionRequest opinionreq) {
        return opinionService.submitOpinion(caseId, doctorId, opinionreq);

    }
    }
