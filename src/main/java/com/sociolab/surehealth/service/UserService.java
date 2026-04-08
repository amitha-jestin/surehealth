package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterResponse;

public interface UserService {

    UserRegisterResponse registerUser(UserRegisterRequest request);
}
