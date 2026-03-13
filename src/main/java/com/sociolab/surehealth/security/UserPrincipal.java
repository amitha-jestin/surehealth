package com.sociolab.surehealth.security;


import com.sociolab.surehealth.enums.Role;

public record UserPrincipal(Long userId, String email, Role role , String accessToken) {




}
