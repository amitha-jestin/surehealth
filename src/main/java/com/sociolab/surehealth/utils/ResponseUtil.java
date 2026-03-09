package com.sociolab.surehealth.utils;

import com.sociolab.surehealth.dto.BaseResponse;
import com.sociolab.surehealth.dto.PagedResponse;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;

public final class ResponseUtil {

    private static final String TRACE_ID = "traceId";
    private static final String VERSION = "v1";

    private ResponseUtil() {}

    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.success(
                data,
                MDC.get(TRACE_ID),
                VERSION
        );
    }

    public static BaseResponse<Void> successMessage(String message) {
        return BaseResponse.successMessage(
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