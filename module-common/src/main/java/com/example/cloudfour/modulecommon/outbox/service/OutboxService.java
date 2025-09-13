package com.example.cloudfour.modulecommon.outbox.service;

import com.example.cloudfour.modulecommon.outbox.entity.OutboxEvent;
import com.example.cloudfour.modulecommon.outbox.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void saveEvent(String aggregateId, String aggregateType, String eventType, 
                         Object eventData, String topic, String key) {
        try {
            String eventDataJson = objectMapper.writeValueAsString(eventData);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(aggregateId)
                    .aggregateType(aggregateType)
                    .eventType(eventType)
                    .eventData(eventDataJson)
                    .topic(topic)
                    .key(key)
                    .createdAt(Instant.now())
                    .status(com.example.cloudfour.modulecommon.outbox.enums.EventStatus.PENDING)
                    .build();
            
            outboxEventRepository.save(outboxEvent);
            log.debug("Outbox 이벤트 저장 완료: aggregateId={}, eventType={}", aggregateId, eventType);
            
        } catch (JsonProcessingException e) {
            log.error("Outbox 이벤트 JSON 변환 실패: aggregateId={}, eventType={}", aggregateId, eventType, e);
            throw new RuntimeException("이벤트 데이터 JSON 변환 실패", e);
        } catch (Exception e) {
            log.error("Outbox 이벤트 저장 실패: aggregateId={}, eventType={}", aggregateId, eventType, e);
            throw e;
        }
    }
    
    @Transactional
    public void markEventAsPublished(UUID eventId) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.markAsProcessed();
            outboxEventRepository.save(event);
            log.debug("Outbox 이벤트 발행 완료 표시: eventId={}", eventId);
        });
    }
    
    @Transactional
    public void markEventAsFailed(UUID eventId, String errorMessage) {
        outboxEventRepository.findById(eventId).ifPresent(event -> {
            event.markAsFailed(errorMessage);
            outboxEventRepository.save(event);
            log.debug("Outbox 이벤트 실패 표시: eventId={}, error={}", eventId, errorMessage);
        });
    }
}


