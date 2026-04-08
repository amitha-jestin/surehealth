package com.sociolab.surehealth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.utils.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final ProblemDetailFactory problemDetailFactory;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String user = authentication != null ? authentication.getName() : "anonymous";

        // ✅ LOG HERE
        log.warn("action=auth_access_denied status=FAILED path={} userId={} message={}",
                request.getRequestURI(),
                user,
                ex.getMessage());

        ProblemDetail problem = ProblemDetailFactory.builder()
                .errorType(ErrorType.ACCESS_DENIED)
                .detail(ex.getMessage() != null
                        ? ex.getMessage()
                        : "You do not have permission to access this resource")
                .path(request.getRequestURI())
                .baseUri(problemDetailFactory.getBaseUri())
                .apiVersion(problemDetailFactory.getApiVersion())
                .clock(problemDetailFactory.getClock())
                .build();

        response.setStatus(problem.getStatus());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
