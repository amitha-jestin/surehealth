package com.sociolab.surehealth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.JwtAuthenticationException;
import com.sociolab.surehealth.exception.utils.ProblemDetailFactory;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthEntryPointTest {

    @Test
    @DisplayName("shouldReturnUnauthorized_whenGenericAuthenticationException")
    void shouldReturnUnauthorized_whenGenericAuthenticationException() throws Exception {
        ProblemDetailFactory factory = new ProblemDetailFactory("https://surehealth/errors/", "v1", Clock.systemUTC());
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint(factory, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cases");
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthenticationException ex = new AuthenticationException("bad") {};

        entryPoint.commence(request, response, ex);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Authentication failed"));
    }

    @Test
    @DisplayName("shouldUseJwtErrorType_whenJwtAuthenticationException")
    void shouldUseJwtErrorType_whenJwtAuthenticationException() throws Exception {
        ProblemDetailFactory factory = new ProblemDetailFactory("https://surehealth/errors/", "v1", Clock.systemUTC());
        JwtAuthEntryPoint entryPoint = new JwtAuthEntryPoint(factory, new ObjectMapper());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cases");
        MockHttpServletResponse response = new MockHttpServletResponse();

        JwtAuthenticationException ex = new JwtAuthenticationException(ErrorType.JWT_INVALID_TOKEN, "Invalid token");

        entryPoint.commence(request, response, ex);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentAsString().contains("Invalid token"));
        assertTrue(response.getContentAsString().contains(ErrorType.JWT_INVALID_TOKEN.name()));
    }
}
