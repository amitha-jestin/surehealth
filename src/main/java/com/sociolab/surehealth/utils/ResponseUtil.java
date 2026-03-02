package com.sociolab.surehealth.utils;

import com.sociolab.surehealth.dto.ApiResponse;
import com.sociolab.surehealth.dto.PagedResponse;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;

import java.util.List;

public final class ResponseUtil {

    private static final String TRACE_ID = "traceId";
    private static final String VERSION = "v1";

    private ResponseUtil() {}

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.success(
                data,
                MDC.get(TRACE_ID),
                VERSION
        );
    }

    public static ApiResponse<Void> successMessage(String message) {
        return ApiResponse.successMessage(
                message,
                MDC.get(TRACE_ID),
                VERSION
        );
    }

    public static <T> PagedResponse<T> paged(Page<T> page) {
        return PagedResponse.of(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                MDC.get(TRACE_ID),
                VERSION
        );
    }
}