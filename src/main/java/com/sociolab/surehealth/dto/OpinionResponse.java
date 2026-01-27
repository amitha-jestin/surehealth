package com.sociolab.surehealth.dto;

public record OpinionResponse (Long id,Long doctorId, Long patientId , String opinionText) {
}
