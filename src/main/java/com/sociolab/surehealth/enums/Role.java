package com.sociolab.surehealth.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum Role {
PATIENT,
    PENDING_DOCTOR, // for doctors awaiting admin approval
DOCTOR,
ADMIN
  /*  @JsonCreator
    public static Role from(String value) {
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Role must be one of: PATIENT, DOCTOR, ADMIN"));
    }
*/
    }
