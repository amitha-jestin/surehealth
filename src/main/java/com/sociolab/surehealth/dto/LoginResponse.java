package com.sociolab.surehealth.dto;

public record LoginResponse(        String token,
                                    String email,
                                    String role
) {
}
