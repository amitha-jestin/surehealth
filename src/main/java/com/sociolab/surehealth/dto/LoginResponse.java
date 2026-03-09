package com.sociolab.surehealth.dto;

public record LoginResponse(        String token,
                                    String refreshToken,
                                    Long id,
                                    String email,
                                    String role
) {
}
