package com.sociolab.surehealth.model;

import com.sociolab.surehealth.enums.NotificationEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationEventType eventType;

    private boolean readStatus = false;

    private LocalDateTime createdAt = LocalDateTime.now();

}
