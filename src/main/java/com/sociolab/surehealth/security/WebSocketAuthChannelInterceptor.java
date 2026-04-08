package com.sociolab.surehealth.security;

import com.sociolab.surehealth.exception.custom.JwtAuthenticationException;
import com.sociolab.surehealth.enums.ErrorType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Channel interceptor that authenticates WebSocket/STOMP CONNECT frames using the application's JWT.
 * - Reads the Authorization header from STOMP native headers ("Authorization: Bearer <token>")
 * - Validates the token with JwtUtil and sets a {@link StompPrincipal} as the Principal on the message so
 *   Spring's messaging infrastructure recognizes the authenticated user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Typical location: native headers "Authorization" (sent by SockJS/STOMP libs when configured)
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            String token = null;

            if (authHeaders != null && !authHeaders.isEmpty()) {
                token = authHeaders.get(0);
            } else {
                // Some clients send the token in sec-websocket-protocol or a custom header; try common fallbacks
                List<String> proto = accessor.getNativeHeader("sec-websocket-protocol");
                if (proto != null && !proto.isEmpty()) {
                    token = proto.get(0);
                }
            }

            if (token != null) {
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                try {
                    Claims claims = jwtUtil.extractAllClaims(token);
                    Long userId = claims.get("id", Long.class);
                    String email = claims.getSubject();
                    String role = claims.get("role", String.class);

                    // set authenticated principal for subsequent messages
                    StompPrincipal principal = new StompPrincipal(userId, email, role, token);
                    accessor.setUser(principal);

                    log.debug("action=ws_auth status=SUCCESS userId={} email={}", userId, email);
                } catch (Exception ex) {
                    // Invalid token — reject the connection by throwing authentication exception
                    log.warn("action=ws_auth status=FAILED reason=INVALID_TOKEN message={}", ex.getMessage());
                    throw new JwtAuthenticationException(ErrorType.JWT_INVALID_TOKEN, "Invalid or expired WebSocket token");
                }
            } else {
                log.warn("action=ws_auth status=FAILED reason=MISSING_TOKEN");
                throw new JwtAuthenticationException(ErrorType.JWT_INVALID_TOKEN, "WebSocket token is required");
            }
        }

        return message;
    }
}



