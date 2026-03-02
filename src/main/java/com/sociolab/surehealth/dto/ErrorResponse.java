package com.sociolab.surehealth.dto;


public record ErrorResponse(String errorCode, String message, int status, String path, long timestamp) {

}
