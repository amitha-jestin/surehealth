package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.DoctorRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.dto.UserRegisterResponse;

public interface UserService {

    UserRegisterResponse registerPatient(UserRegisterRequest req);

    UserRegisterResponse registerDoctor(DoctorRegisterRequest req);
}
