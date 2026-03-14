package com.sociolab.surehealth.listener;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import com.sociolab.surehealth.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationConsumer {

    private final NotificationService notificationService;
    private final KafkaTemplate<String, CaseNotificationEvent> kafkaTemplate;

    // Dead-letter topic where failed events will be forwarded for later inspection/processing
    @Value("${kafka.dlt-topic:case-notification-dlt}")
    private String dltTopic = "case-notification-dlt";

    @KafkaListener(
            topics = "case-notification-topic",
            groupId = "surehealth-group"
    )
    public void consume(CaseNotificationEvent event) {

        log.info("Kafka event received for userId={} eventType={}", event.getUserId(), event.getEventType());

        try {
            // Delegate to NotificationService which handles persistence and websocket delivery
            // Use synchronous call so any failures are thrown to the surrounding catch and can be DLT'd
            notificationService.sendCaseNotificationSync(
                    event.getUserId(),
                    event.getMessage(),
                    event.getEventType()
            );

        } catch (Exception e) {
            // Log the failure with context
            log.error("Failed to process Kafka event for userId={}, attempting to forward to DLT ({}). error={}",
                    event.getUserId(), dltTopic, e.getMessage(), e);

            // Attempt to forward the original event to a dead-letter topic so it can be retried or inspected
            try {
                var future = kafkaTemplate.send(dltTopic, event);
                try {
                    // Wait briefly for send metadata so we can log partition info; do not block indefinitely
                    SendResult<String, CaseNotificationEvent> result = future.get(5, TimeUnit.SECONDS);
                    if (result != null && result.getRecordMetadata() != null) {
                        log.debug("Event forwarded to DLT topic={} for userId={} partition={}",
                                dltTopic, event.getUserId(), result.getRecordMetadata().partition());
                    } else {
                        log.debug("Event forwarded to DLT topic={} for userId={} (no metadata)", dltTopic, event.getUserId());
                    }
                } catch (TimeoutException te) {
                    log.warn("Timed out while forwarding event to DLT={} for userId={} - send will continue asynchronously", dltTopic, event.getUserId());
                } catch (Exception ex) {
                    log.error("Failed to forward event to DLT={} for userId={} error={}", dltTopic, event.getUserId(), ex.getMessage(), ex);
                }

            } catch (Exception sendEx) {
                // If we cannot forward to DLT, log the error and swallow to avoid infinite retry loops at the listener level
                log.error("Critical: failed to send event to DLT={} for userId={} error={}", dltTopic, event.getUserId(), sendEx.getMessage(), sendEx);
            }
        }
    }
}
