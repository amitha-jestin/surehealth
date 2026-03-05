package com.sociolab.surehealth.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String traceId = httpRequest.getHeader(TRACE_HEADER);

            if (traceId == null || traceId.isBlank()) {
                traceId = UUID.randomUUID().toString();
            }

            MDC.put(TRACE_ID, traceId);
            httpResponse.setHeader(TRACE_HEADER, traceId);

            chain.doFilter(request, response);

        } finally {
            MDC.clear();
        }
    }
}
