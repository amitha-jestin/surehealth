package com.sociolab.surehealth.dto;

public record ApiResponse(String status, String message) {


    public static ApiResponse success(String message) {
        return new ApiResponse("success", message);
    }

    public static ApiResponse fail(String message) {
        return new ApiResponse("Failure", message);
    }

}
