package com.sociolab.surehealth.security;

import com.sociolab.surehealth.exception.custom.JwtAuthenticationException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Test
    @DisplayName("shouldSetPrincipal_whenValidTokenProvided")
    void shouldSetPrincipal_whenValidTokenProvided() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Claims claims = mock(Claims.class);
        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(claims.get("id", Long.class)).thenReturn(10L);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("role", String.class)).thenReturn("DOCTOR");

        Message<?> result = interceptor.preSend(message, mock(MessageChannel.class));
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);

        assertNotNull(result);
        verify(jwtUtil).extractAllClaims("token");
        assertDoesNotThrow(() -> resultAccessor.getUser());
    }

    @Test
    @DisplayName("shouldThrowException_whenTokenMissing")
    void shouldThrowException_whenTokenMissing() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(JwtAuthenticationException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class)));
    }

    @Test
    @DisplayName("shouldThrowException_whenTokenInvalid")
    void shouldThrowException_whenTokenInvalid() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtUtil.extractAllClaims("token")).thenThrow(new RuntimeException("bad"));

        assertThrows(JwtAuthenticationException.class,
                () -> interceptor.preSend(message, mock(MessageChannel.class)));
    }
}
