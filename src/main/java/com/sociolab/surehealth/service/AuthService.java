package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;
import com.sociolab.surehealth.dto.RefreshTokenResponse;
import com.sociolab.surehealth.security.UserPrincipal;

import java.util.Map;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout(Long userId, String accessToken);

    RefreshTokenResponse refreshAccessToken(String refreshToken);
}
