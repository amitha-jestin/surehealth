package com.sociolab.surehealth.exception.custom;

import com.sociolab.surehealth.enums.ErrorType;

public class AppException extends RuntimeException {

    private final ErrorType errorType;
    private final String detailMessage;

    public AppException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
        this.detailMessage = null;
    }

    public AppException(ErrorType errorType, String detailMessage) {
        super(detailMessage != null ? detailMessage : errorType.getMessage());
        this.errorType = errorType;
        this.detailMessage = detailMessage;
    }

    public AppException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
        this.detailMessage = null;
    }

    public AppException(ErrorType errorType, String detailMessage, Throwable cause) {
        super(detailMessage != null ? detailMessage : errorType.getMessage(), cause);
        this.errorType = errorType;
        this.detailMessage = detailMessage;
    }

    public ErrorType getErrorType() { return errorType; }
    public int getHttpCode() { return errorType.getHttpCode(); }
    public String getDetailMessage() { return detailMessage; }
}

