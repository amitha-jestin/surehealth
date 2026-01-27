package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.enums.Role;
import com.sociolab.surehealth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);

  //  List<User> findByRole(Role role);

    Page<User> findByRole(Role role, Pageable pageable);

}
