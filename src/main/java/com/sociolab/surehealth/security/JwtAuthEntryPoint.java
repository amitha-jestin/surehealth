package com.sociolab.surehealth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.JwtAuthenticationException;
import com.sociolab.surehealth.exception.utils.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    private final ProblemDetailFactory problemDetailFactory;
    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        ErrorType errorType = ErrorType.UNAUTHORIZED;
        String detailMessage = "Authentication failed";

        if (ex instanceof JwtAuthenticationException jwtEx) {
            errorType = jwtEx.getErrorType();
            detailMessage = jwtEx.getMessage();
        }

        // ✅ LOG HERE (before building response)
        log.warn(
                "AUTH_FAILED path={} errorType={} message={}",
                request.getRequestURI(),
                errorType,
                detailMessage
        );

        ProblemDetail problemDetail = ProblemDetailFactory.builder()
                .errorType(errorType)
                .detail(detailMessage)
                .path(request.getRequestURI())
                .baseUri(problemDetailFactory.getBaseUri())
                .apiVersion(problemDetailFactory.getApiVersion())
                .clock(problemDetailFactory.getClock())
                .build();

        response.setStatus(problemDetail.getStatus());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}