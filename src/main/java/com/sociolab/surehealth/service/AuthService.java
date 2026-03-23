package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.LoginRequest;
import com.sociolab.surehealth.dto.LoginResponse;

import java.util.Map;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout();

    Map<String, String> refreshAccessToken(String refreshToken);
}
