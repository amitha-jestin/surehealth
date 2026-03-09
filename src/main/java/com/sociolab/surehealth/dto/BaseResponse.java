package com.sociolab.surehealth.dto;

import java.time.Instant;
import java.util.List;

public record BaseResponse<T>(
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

    public static <T> BaseResponse<T> of(T data, String message, String traceId, String version, List<String> warnings) {
        return new BaseResponse<>(
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

    public static <T> BaseResponse<T> success(T data, String traceId, String version) {
        return of(data, "success", traceId, version, List.of());
    }

    public static <T> BaseResponse<T> successMessage(String message, String traceId, String version) {
        return of(null, message, traceId, version, List.of());
    }

    public static <T> BaseResponse<T> successWithWarnings(T data, List<String> warnings, String traceId, String version) {
        return of(data, "success", traceId, version, warnings);
    }
}