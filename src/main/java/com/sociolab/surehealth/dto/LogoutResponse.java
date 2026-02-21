package com.sociolab.surehealth.dto;

import java.time.LocalDateTime;

public record LogoutResponse(int status, String message, LocalDateTime timestamp) {
}
