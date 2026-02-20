package com.sociolab.surehealth.listener;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import com.sociolab.surehealth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
//@Slf4j
public class KafkaNotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "case-notification-topic",
            groupId = "surehealth-group"
    )
    public void consume(CaseNotificationEvent event) {

       // log.info("Kafka event received for user {}", event.getUserId());

        notificationService.sendCaseNotification(
                event.getUserId(),
                event.getMessage(),
                event.getEventType()
        );
    }
}
