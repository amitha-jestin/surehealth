package com.sociolab.surehealth.service;

import com.sociolab.surehealth.dto.CaseNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
//@Slf4j
public class KafkaNotificationProducer {

    private final KafkaTemplate<String, CaseNotificationEvent> kafkaTemplate;

    private static final String TOPIC = "case-notification-topic";

    public void sendEvent(CaseNotificationEvent event) {
    //    log.info("Publishing Kafka event for user {}", event.getUserId());
        kafkaTemplate.send(TOPIC, event);
    }
}
