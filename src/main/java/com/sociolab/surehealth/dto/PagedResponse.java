package com.sociolab.surehealth.dto;

import java.time.Instant;
import java.util.List;

public record PagedResponse<T>(
        List<T> content,
        PageMeta meta
) {

    public record PageMeta(
            int page,
            int size,
            long totalElements,
            int totalPages,
            Instant timestamp,
            String traceId,
            String version
    ) {}

    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements, int totalPages,
                                          String traceId, String version) {
        return new PagedResponse<>(
                content,
                new PageMeta(page, size, totalElements, totalPages, Instant.now(), traceId, version)
        );
    }
}