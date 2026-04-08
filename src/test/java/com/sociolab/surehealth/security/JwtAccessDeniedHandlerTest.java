package com.sociolab.surehealth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.exception.utils.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAccessDeniedHandlerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("shouldReturnForbidden_whenAccessDenied")
    void shouldReturnForbidden_whenAccessDenied() throws Exception {
        ProblemDetailFactory factory = new ProblemDetailFactory("https://surehealth/errors/", "v1", Clock.systemUTC());
        JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(new ObjectMapper(), factory);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("denied"));

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
        assertTrue(response.getContentAsString().contains("ACCESS_DENIED"));
    }
}
