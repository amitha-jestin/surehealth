package com.sociolab.surehealth.listener;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import com.sociolab.surehealth.service.KafkaNotificationProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Forwards application events to the Kafka producer after transaction commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaNotificationEventListener {

    private final KafkaNotificationProducer producer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCaseNotification(CaseNotificationEvent event) {
        log.debug("Forwarding CaseNotificationEvent to Kafka: userId={} type={}", event.getUserId(), event.getEventType());
        try {
            producer.sendEvent(event);
        } catch (Exception ex) {
            log.error("Failed to send CaseNotificationEvent to Kafka for userId={}", event.getUserId(), ex);
        }
    }
}
