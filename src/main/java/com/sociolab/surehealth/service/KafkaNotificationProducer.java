package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationProducer {

    private final KafkaTemplate<String, CaseNotificationEvent> kafkaTemplate;

    private static final String TOPIC = "case-notification-topic";

    public void sendEvent(CaseNotificationEvent event) {
       log.info("action=kafka_notification_publish status=START userId={} eventId={}", event.getUserId(), event.getEventId());
        CompletableFuture<SendResult<String, CaseNotificationEvent>> future = kafkaTemplate.send(TOPIC, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("action=kafka_notification_publish status=FAILED userId={} eventId={} error={}",
                        event.getUserId(), event.getEventId(), ex.getMessage(), ex);
                return;
            }
            if (result != null && result.getRecordMetadata() != null) {
                log.info("action=kafka_notification_publish status=SUCCESS userId={} eventId={} partition={} offset={}",
                        event.getUserId(), event.getEventId(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.info("action=kafka_notification_publish status=SUCCESS userId={} eventId={} metadata=NONE",
                        event.getUserId(), event.getEventId());
            }
        });
    }
}
