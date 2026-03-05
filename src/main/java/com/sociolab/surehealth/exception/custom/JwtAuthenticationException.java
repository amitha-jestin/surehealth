package com.sociolab.surehealth.exception.custom;

import com.sociolab.surehealth.enums.ErrorType;
import org.springframework.security.core.AuthenticationException;

public class JwtAuthenticationException extends AuthenticationException {

    private final ErrorType errorType;

    public JwtAuthenticationException(ErrorType errorType, String message) {
        super(message != null ? message : errorType.getMessage());
        this.errorType = errorType;
    }

    public JwtAuthenticationException(ErrorType errorType, String message, Throwable cause) {
        super(message != null ? message : errorType.getMessage(), cause);
        this.errorType = errorType;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public int getHttpCode() {
        return errorType.getHttpCode();
    }
}
