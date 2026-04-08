package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.dto.RefreshTokenResponse;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.security.UserPrincipal;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenService tokenService;
    @Mock
    private RefreshTokenStore refreshTokenStore;
    @Mock
    private LoginAttemptPolicy loginAttemptPolicy;
    @Mock
    private LoginRateLimiter loginRateLimiter;
    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("shouldReturnLoginResponse_whenCredentialsValid")
    void shouldReturnLoginResponse_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("user@example.com", "Password1!");
        User user = buildUser(1L, "user@example.com", Role.PATIENT, AccountStatus.ACTIVE);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(true);
        when(tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("access");
        when(tokenService.generateRefreshToken(user.getId())).thenReturn("refresh");
        when(refreshTokenHasher.hash("refresh")).thenReturn("hashed");
        when(tokenService.getRefreshExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(request);

        assertEquals("access", response.token());
        assertEquals("refresh", response.refreshToken());
        assertEquals(user.getId(), response.id());
        assertEquals(user.getEmail(), response.email());
        assertEquals(user.getRole().name(), response.role());

        verify(loginRateLimiter).checkAllowed(request.email());
        verify(loginAttemptPolicy).validateLoginAllowed(user);
        verify(loginAttemptPolicy).onSuccessfulAttempt(user);
        verify(refreshTokenStore).saveRefreshToken(user.getId(), "hashed", 3600L);
    }

    @Test
    @DisplayName("shouldThrowInvalidCredentials_whenUserNotFound")
    void shouldThrowInvalidCredentials_whenUserNotFound() {
        LoginRequest request = new LoginRequest("missing@example.com", "Password1!");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        verify(loginRateLimiter).checkAllowed(request.email());
        verify(loginAttemptPolicy, never()).validateLoginAllowed(any());
    }

    @Test
    @DisplayName("shouldThrowInvalidCredentials_whenPasswordMismatch")
    void shouldThrowInvalidCredentials_whenPasswordMismatch() {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPass!");
        User user = buildUser(1L, "user@example.com", Role.PATIENT, AccountStatus.ACTIVE);

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPassword())).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));

        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        verify(loginAttemptPolicy).validateLoginAllowed(user);
        verify(loginAttemptPolicy).onFailedAttempt(user);
    }

    @Test
    @DisplayName("shouldBlacklistAndDeleteRefreshToken_whenLogoutWithToken")
    void shouldBlacklistAndDeleteRefreshToken_whenLogoutWithToken() {


        when(tokenService.getRemainingExpirationSeconds("token")).thenReturn(120L);

        authService.logout(42L, "token");

        verify(refreshTokenStore).blacklistAccessToken("token", 120L);
        verify(refreshTokenStore).deleteRefreshToken(42L);
    }

    @Test
    @DisplayName("shouldNotBlacklist_whenLogoutWithoutToken")
    void shouldNotBlacklist_whenLogoutWithoutToken() {


        authService.logout(42L, null);

        verifyNoInteractions(refreshTokenStore);
    }

    @Test
    @DisplayName("shouldReturnNewTokens_whenRefreshTokenValid")
    void shouldReturnNewTokens_whenRefreshTokenValid() {
        String refreshToken = "refresh";
        User user = buildUser(10L, "user@example.com", Role.PATIENT, AccountStatus.ACTIVE);

        when(tokenService.extractUserId(refreshToken)).thenReturn(10L);
        when(refreshTokenStore.getRefreshToken(10L)).thenReturn("hashed");
        when(refreshTokenHasher.hash(refreshToken)).thenReturn("hashed");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(tokenService.generateAccessToken(user.getId(), user.getEmail(), user.getRole())).thenReturn("newAccess");
        when(tokenService.generateRefreshToken(user.getId())).thenReturn("newRefresh");
        when(refreshTokenHasher.hash("newRefresh")).thenReturn("newHashed");
        when(tokenService.getRefreshExpirationSeconds()).thenReturn(600L);

        RefreshTokenResponse result = authService.refreshAccessToken(refreshToken);

        assertEquals("newAccess", result.accessToken());
        assertEquals("newRefresh", result.refreshToken());
        verify(tokenService).validateRefreshToken(refreshToken);
        verify(refreshTokenStore).saveRefreshToken(user.getId(), "newHashed", 600L);
    }

    @Test
    @DisplayName("shouldRevokeAllTokens_whenStoredRefreshTokenMismatch")
    void shouldRevokeAllTokens_whenStoredRefreshTokenMismatch() {
        String refreshToken = "refresh";
        when(tokenService.extractUserId(refreshToken)).thenReturn(10L);
        when(refreshTokenStore.getRefreshToken(10L)).thenReturn("stored");
        when(refreshTokenHasher.hash(refreshToken)).thenReturn("hashed");

        AppException ex = assertThrows(AppException.class, () -> authService.refreshAccessToken(refreshToken));

        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
        verify(refreshTokenStore).revokeAllRefreshTokens(10L);
    }

    @Test
    @DisplayName("shouldThrowInvalidCredentials_whenRefreshTokenMissing")
    void shouldThrowInvalidCredentials_whenRefreshTokenMissing() {
        AppException ex = assertThrows(AppException.class, () -> authService.refreshAccessToken(""));
        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowInvalidCredentials_whenRefreshTokenInvalid")
    void shouldThrowInvalidCredentials_whenRefreshTokenInvalid() {
        doThrow(new RuntimeException("invalid")).when(tokenService).validateRefreshToken("bad");

        AppException ex = assertThrows(AppException.class, () -> authService.refreshAccessToken("bad"));

        assertEquals(ErrorType.INVALID_CREDENTIALS, ex.getErrorType());
    }

    @Test
    @DisplayName("shouldThrowResourceNotFound_whenUserMissingOnRefresh")
    void shouldThrowResourceNotFound_whenUserMissingOnRefresh() {
        String refreshToken = "refresh";
        when(tokenService.extractUserId(refreshToken)).thenReturn(10L);
        when(refreshTokenStore.getRefreshToken(10L)).thenReturn("hashed");
        when(refreshTokenHasher.hash(refreshToken)).thenReturn("hashed");
        when(userRepository.findById(10L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> authService.refreshAccessToken(refreshToken));

        assertEquals(ErrorType.RESOURCE_NOT_FOUND, ex.getErrorType());
    }

    private User buildUser(Long id, String email, Role role, AccountStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        user.setPassword("encoded");
        return user;
    }
}
