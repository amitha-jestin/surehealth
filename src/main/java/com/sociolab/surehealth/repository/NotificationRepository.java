package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdAndReadStatus(Long userId, boolean readStatus, Pageable pageable);
}
