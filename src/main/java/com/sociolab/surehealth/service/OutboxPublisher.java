package com.sociolab.surehealth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.dto.CaseNotificationEvent;
import com.sociolab.surehealth.model.OutboxEvent;
import com.sociolab.surehealth.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaNotificationProducer kafkaNotificationProducer;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:5000}")
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc("PENDING");
        for (OutboxEvent event : pending) {
            publishSingleEvent(event);
        }
    }

    @Transactional
    protected void publishSingleEvent(OutboxEvent event) {
        try {
            if ("CASE_ASSIGNED".equals(event.getEventType())
                    || "CASE_ACCEPTED".equals(event.getEventType())
                    || "CASE_REJECTED".equals(event.getEventType())
                    || "CASE_STATUS_UPDATED".equals(event.getEventType())
                    || "CASE_CREATED".equals(event.getEventType())
                    || "CASE_REVIEWED".equals(event.getEventType())) {
                CaseNotificationEvent payload = objectMapper.readValue(event.getPayload(), CaseNotificationEvent.class);
                kafkaNotificationProducer.sendEvent(payload);
            }

            event.setStatus("SENT");
            event.setLastAttemptAt(LocalDateTime.now());
            outboxEventRepository.save(event);

        } catch (Exception ex) {
            log.error("action=outbox_publish status=FAILED eventId={} eventType={}", event.getId(), event.getEventType(), ex);
            event.setAttempts(event.getAttempts() + 1);
            event.setLastAttemptAt(LocalDateTime.now());
            if (event.getAttempts() >= 5) {
                event.setStatus("FAILED");
            }
            outboxEventRepository.save(event);
        }
    }
}
