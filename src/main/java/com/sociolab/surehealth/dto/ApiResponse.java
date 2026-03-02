package com.sociolab.surehealth.dto;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        T data,
        Meta meta
) {

    public record Meta(
            String message,
            Instant timestamp,
            String traceId,
            String version,
            List<String> warnings
    ) {}

    public static <T> ApiResponse<T> of(T data, String message, String traceId, String version, List<String> warnings) {
        return new ApiResponse<>(
                data,
                new Meta(
                        message,
                        Instant.now(),
                        traceId,
                        version,
                        warnings
                )
        );
    }

    public static <T> ApiResponse<T> success(T data, String traceId, String version) {
        return of(data, "success", traceId, version, List.of());
    }

    public static <T> ApiResponse<T> successMessage(String message, String traceId, String version) {
        return of(null, message, traceId, version, List.of());
    }

    public static <T> ApiResponse<T> successWithWarnings(T data, List<String> warnings, String traceId, String version) {
        return of(data, "success", traceId, version, warnings);
    }
}