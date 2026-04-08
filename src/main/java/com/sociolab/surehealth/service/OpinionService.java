package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.OpinionRequest;
import com.sociolab.surehealth.dto.OpinionResponse;

public interface OpinionService {

    OpinionResponse submitOpinion(Long caseId, Long doctorId, OpinionRequest request);
}
