package com.sociolab.surehealth.service;

import com.sociolab.surehealth.model.User;

public interface LoginAttemptPolicy {

    void validateLoginAllowed(User user);

    void onFailedAttempt(User user);

    void onSuccessfulAttempt(User user);
}
