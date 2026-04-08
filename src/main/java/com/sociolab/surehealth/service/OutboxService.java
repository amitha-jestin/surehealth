package com.sociolab.surehealth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sociolab.surehealth.enums.ErrorType;
import com.sociolab.surehealth.exception.custom.AppException;
import com.sociolab.surehealth.model.OutboxEvent;
import com.sociolab.surehealth.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void enqueue(String eventType, String aggregateType, String aggregateId, Object payload) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(payload);
            OutboxEvent event = new OutboxEvent();
            event.setEventType(eventType);
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setPayload(jsonPayload);
            outboxEventRepository.save(event);
        } catch (JsonProcessingException ex) {
            log.error("action=outbox_enqueue status=FAILED eventType={} aggregateId={}", eventType, aggregateId, ex);
            throw new AppException(ErrorType.INTERNAL_SERVER_ERROR, "Failed to serialize outbox payload", ex);
        }
    }
}
