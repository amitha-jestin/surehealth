package com.sociolab.surehealth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.ErrorResponse;
import com.sociolab.surehealth.service.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            ObjectMapper objectMapper,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * ✅ Skip auth endpoints + websocket + swagger
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/ws")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {

            String authHeader = request.getHeader("Authorization");

            // ✅ No token → continue (public endpoints may exist)
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);

            // 🔥 CRITICAL: check blacklist FIRST
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.warn("Blocked blacklisted JWT");

                writeError(response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "JWT_BLACKLISTED",
                        "Token has been logged out. Please login again.");
                return;
            }

            // ✅ Avoid re-authentication
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // ✅ Parse token
            Claims claims = jwtUtil.extractAllClaims(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            var authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + role)
            );

            var authentication = new UsernamePasswordAuthenticationToken(
                    email,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (JwtException ex) {

            log.warn("JWT validation failed: {}", ex.getMessage());

            writeError(response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "JWT_INVALID",
                    "Invalid or expired authentication token");

        } catch (Exception ex) {

            log.error("Authentication error", ex);

            writeError(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "AUTH_ERROR",
                    "Authentication processing failed");
        }
    }

    /**
     * ✅ Standardized error writer
     */
    private void writeError(HttpServletResponse response,
                            int status,
                            String code,
                            String message) throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        objectMapper.writeValue(
                response.getOutputStream(),
                new ErrorResponse(code, message)
        );
    }
}
