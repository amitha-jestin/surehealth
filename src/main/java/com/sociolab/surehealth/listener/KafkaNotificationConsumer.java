package com.sociolab.surehealth.listener;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import com.sociolab.surehealth.service.NotificationService;
import com.sociolab.surehealth.service.RedisService;
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
    private final RedisService redisService;

    // Dead-letter topic where failed events will be forwarded for later inspection/processing
    @Value("${kafka.dlt-topic:case-notification-dlt}")
    private String dltTopic = "case-notification-dlt";

    @Value("${kafka.consumer.idempotency.ttl-seconds:86400}")
    private long idempotencyTtlSeconds;

    @KafkaListener(
            topics = "case-notification-topic",
            groupId = "surehealth-group"
    )
    public void consume(CaseNotificationEvent event) {

        log.info("action=kafka_notification_consume status=START userId={} eventType={} eventId={}",
                event.getUserId(), event.getNewStatus(), event.getEventId());

        try {
            if (event.getEventId() != null && !event.getEventId().isBlank()) {
                String key = "kafka:case-event:" + event.getEventId();
                boolean firstTime = redisService.setIfAbsent(key, "processed", idempotencyTtlSeconds);
                if (!firstTime) {
                    log.warn("action=kafka_notification_consume status=NOOP reason=DUPLICATE_EVENT eventId={} userId={}",
                            event.getEventId(), event.getUserId());
                    return;
                }
            } else {
                log.warn("action=kafka_notification_consume status=NOOP reason=MISSING_EVENT_ID userId={} eventType={}",
                        event.getUserId(), event.getNewStatus());
            }

            // Delegate to NotificationService which handles persistence and websocket delivery
            // Use synchronous call so any failures are thrown to the surrounding catch and can be DLT'd
            notificationService.sendCaseNotificationSync(
                    event.getUserId(),
                    event.getMessage(),
                    event.getNewStatus()
            );

            log.info("action=kafka_notification_consume status=SUCCESS userId={} eventType={} eventId={}",
                    event.getUserId(), event.getNewStatus(), event.getEventId());

        } catch (Exception e) {
            // Log the failure with context
            log.error("action=kafka_notification_consume status=FAILED userId={} eventId={} dltTopic={} error={}",
                    event.getUserId(), event.getEventId(), dltTopic, e.getMessage(), e);

            // Attempt to forward the original event to a dead-letter topic so it can be retried or inspected
            try {
                var future = kafkaTemplate.send(dltTopic, event);
                try {
                    // Wait briefly for send metadata so we can log partition info; do not block indefinitely
                    SendResult<String, CaseNotificationEvent> result = future.get(5, TimeUnit.SECONDS);
                    if (result != null && result.getRecordMetadata() != null) {
                        log.debug("action=kafka_notification_dlt_forward status=SUCCESS dltTopic={} userId={} eventId={} partition={}",
                                dltTopic, event.getUserId(), event.getEventId(), result.getRecordMetadata().partition());
                    } else {
                        log.debug("action=kafka_notification_dlt_forward status=SUCCESS dltTopic={} userId={} eventId={} metadata=NONE",
                                dltTopic, event.getUserId(), event.getEventId());
                    }
                } catch (TimeoutException te) {
                    log.warn("action=kafka_notification_dlt_forward status=NOOP reason=TIMEOUT dltTopic={} userId={} eventId={}",
                            dltTopic, event.getUserId(), event.getEventId());
                } catch (Exception ex) {
                    log.error("action=kafka_notification_dlt_forward status=FAILED dltTopic={} userId={} eventId={} error={}",
                            dltTopic, event.getUserId(), event.getEventId(), ex.getMessage(), ex);
                }

            } catch (Exception sendEx) {
                // If we cannot forward to DLT, log the error and swallow to avoid infinite retry loops at the listener level
                log.error("action=kafka_notification_dlt_forward status=FAILED dltTopic={} userId={} eventId={} error={}",
                        dltTopic, event.getUserId(), event.getEventId(), sendEx.getMessage(), sendEx);
            }
        }
    }
}
