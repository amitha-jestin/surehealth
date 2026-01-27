package com.sociolab.surehealth.dto;

public record LoginResponse(        String token,
                                    Long id,
                                    String email,
                                    String role
) {
}
