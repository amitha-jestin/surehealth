package com.sociolab.surehealth.security;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.custom.JwtAuthenticationException;
import com.sociolab.surehealth.service.RedisService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisService redisService;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    @Test
    @DisplayName("shouldContinueFilterChain_whenAuthorizationHeaderMissing")
    void shouldContinueFilterChain_whenAuthorizationHeaderMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cases");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil, redisService);
    }

    @Test
    @DisplayName("shouldThrowJwtAuthenticationException_whenTokenBlacklisted")
    void shouldThrowJwtAuthenticationException_whenTokenBlacklisted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cases");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisService.isTokenBlacklisted("token")).thenReturn(true);

        JwtAuthenticationException ex = assertThrows(JwtAuthenticationException.class,
                () -> filter.doFilterInternal(request, response, filterChain));

        assertEquals(ErrorType.JWT_BLACKLISTED, ex.getErrorType());
        verifyNoInteractions(jwtUtil);
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("shouldSetAuthenticationAndContinue_whenTokenValid")
    void shouldSetAuthenticationAndContinue_whenTokenValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cases");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(redisService.isTokenBlacklisted("token")).thenReturn(false);
        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(claims.get("id", Long.class)).thenReturn(1L);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");
        when(jwtUtil.parseRole("PATIENT")).thenReturn(Role.PATIENT);

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication() instanceof UsernamePasswordAuthenticationToken);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("shouldThrowJwtAuthenticationException_whenClaimsMissing")
    void shouldThrowJwtAuthenticationException_whenClaimsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cases");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        Claims claims = mock(Claims.class);
        when(redisService.isTokenBlacklisted("token")).thenReturn(false);
        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(claims.get("id", Long.class)).thenReturn(null);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("role", String.class)).thenReturn("PATIENT");

        JwtAuthenticationException ex = assertThrows(JwtAuthenticationException.class,
                () -> filter.doFilterInternal(request, response, filterChain));

        assertEquals(ErrorType.JWT_INVALID_TOKEN, ex.getErrorType());
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("shouldBypassJwtProcessing_whenAlreadyAuthenticated")
    void shouldBypassJwtProcessing_whenAlreadyAuthenticated() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/cases");
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null)
        );

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }
}
