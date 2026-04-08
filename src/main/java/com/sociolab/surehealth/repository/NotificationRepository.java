package com.sociolab.surehealth.repository;

import com.sociolab.surehealth.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Query by nested property (user id)
    Page<Notification> findByUser_IdAndReadStatus(Long userId, boolean readStatus, Pageable pageable);

    Page<Notification> findByUser_Id(Long userId, Pageable pageable);
}
