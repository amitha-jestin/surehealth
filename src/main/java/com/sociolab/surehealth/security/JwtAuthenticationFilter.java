package com.sociolab.surehealth.security;

import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.JwtAuthenticationException;
import com.sociolab.surehealth.service.RedisService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.equals("/api/v1/auth/login")
                || path.equals("/api/v1/auth/refresh")   // auth endpoints
                || path.startsWith("/ws")
                || path.startsWith("/ws/**")         // websocket endpoints
                || path.startsWith("/v3/api-docs/**")
                || path.startsWith("/swagger-ui/**")
                || path.startsWith("/swagger-ui.html/**")
                || path.startsWith("/swagger-resources")
                || path.equals("/api/v1/users")  // user registration endpoint
                || path.startsWith("/webjars/**")
                || path.startsWith("/actuator/health")
                || path.startsWith("actuator/info"); // actuator endpoints
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Check if token is blacklisted
            if (redisService.isTokenBlacklisted(token)) {
                log.warn("action=jwt_auth status=FAILED reason=TOKEN_BLACKLISTED path={}", request.getRequestURI());
                throw new JwtAuthenticationException(
                        ErrorType.JWT_BLACKLISTED,
                        "Token has been logged out"
                );
            }

            // Skip if already authenticated
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Parse and validate token
            Claims claims = jwtUtil.extractAllClaims(token);

            Long userId = claims.get("id", Long.class); // extract userId
            String email = claims.getSubject();
            String role = claims.get("role", String.class);

            if (userId == null || email == null || role == null) {
                log.warn("action=jwt_auth status=FAILED reason=INVALID_CLAIMS path={}", request.getRequestURI());
                throw new JwtAuthenticationException(
                        ErrorType.JWT_INVALID_TOKEN,
                        "Invalid JWT claims"
                );
            }

            // Create UserPrincipal for SecurityContext
            UserPrincipal principal = new UserPrincipal(userId, email, jwtUtil.parseRole(role), token);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.debug("action=jwt_auth status=SUCCESS userId={} path={}", userId, request.getRequestURI());

            filterChain.doFilter(request, response);

        } catch (JwtException ex) {
            log.warn("action=jwt_auth status=FAILED reason=INVALID_TOKEN path={} message={}", request.getRequestURI(), ex.getMessage());
            throw new JwtAuthenticationException(
                    ErrorType.JWT_INVALID_TOKEN,
                    "Invalid or expired token"
            );
        } catch (Exception ex) {
            log.error("action=jwt_auth status=FAILED reason=PROCESSING_ERROR path={}", request.getRequestURI(), ex);
            throw ex;
        }
    }
}

