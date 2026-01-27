package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.UserRegisterRequest;
import com.sociolab.surehealth.enums.AccountStatus;
import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.exception.DuplicateResourceException;
import com.sociolab.surehealth.model.User;
import com.sociolab.surehealth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public User register(UserRegisterRequest req) {

        userRepository.findByEmail(req.getEmail())
                .ifPresent(u -> { throw new DuplicateResourceException("Email already exists"); });

        User user = new User();
        user.setName(req.getName());
        user.setEmail(req.getEmail());
        user.setRole(Role.PATIENT); // default
        user.setStatus(AccountStatus.ACTIVE); // default
        user.setPassword(passwordEncoder.encode(req.getPassword()));

        return userRepository.save(user);
    }

    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

}
