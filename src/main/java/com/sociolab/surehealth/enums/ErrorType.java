package com.sociolab.surehealth.enums;

import com.sociolab.surehealth.model.User;

public enum ErrorType {

    // AUTHENTICATION / AUTHORIZATION
    INVALID_CREDENTIALS("Invalid username or password", 401),
    UNAUTHORIZED("Authorization token missing or invalid", 401),
    ACCESS_DENIED("You do not have permission to access this resource", 403),

    JWT_BLACKLISTED("JWT token is blacklisted", 401),
    JWT_INVALID_TOKEN("JWT token is invalid", 401),
    // CLIENT / REQUEST ERRORS
    RESOURCE_NOT_FOUND("Requested resource not found", 404),
    DUPLICATE_RESOURCE("Resource already exists", 409),
    VALIDATION_ERROR("Invalid request data", 400),

    // SYSTEM ERRORS
    INTERNAL_SERVER_ERROR("Internal server error", 500),
    NOTIFICATION_FAILED("Unable to send notification, please try again later", 500),
    USER_ACTIVE("User account is active", 400),
    USER_BLOCKED("User account is locked", 403),
    USER_PENDING("User account is pending activation", 400),
    USER_INVALID_STATUS("User account is in invalid status for this operation", 400),
    INVALID_OPERATION("Invalid operation for the current state", 400),
    DOCUMENT_UPLOAD_FAILED("Failed to upload document, please try again", 500);
    
    private final String message;
    private final int httpCode;

    ErrorType(String message, int httpCode) {
        this.message = message;
        this.httpCode = httpCode;
    }

    public String getMessage() { return message; }
    public int getHttpCode() { return httpCode; }

}
