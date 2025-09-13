package com.example.cloudfour.modulecommon.outbox.publisher;

import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.example.cloudfour.modulecommon.messaging.MsgMeta;
import com.example.cloudfour.modulecommon.converter.MessageConverter;
import com.example.cloudfour.modulecommon.outbox.entity.OutboxEvent;
import com.example.cloudfour.modulecommon.outbox.repository.OutboxEventRepository;
import com.example.cloudfour.modulecommon.outbox.service.OutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true", matchIfMissing = false)
public class OutboxPublisher {
    
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxService outboxService;
    private final KafkaTemplate<String, Envelope<?>> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();
        
        if (pendingEvents.isEmpty()) {
            return;
        }
        
        log.info("Outbox 이벤트 발행 시작: {} 개", pendingEvents.size());
        
        for (OutboxEvent event : pendingEvents) {
            try {
                publishEvent(event);
            } catch (Exception e) {
                log.error("Outbox 이벤트 발행 실패: eventId={}, error={}", 
                        event.getId(), e.getMessage(), e);
                outboxService.markEventAsFailed(event.getId(), e.getMessage());
            }
        }
    }

    private void publishEvent(OutboxEvent event) {
        try {
            Object eventData = objectMapper.readValue(event.getEventData(), Object.class);

            MsgMeta meta = MessageConverter.toMsgMeta(event);
            Envelope<Object> envelope = MessageConverter.toEnvelope(meta, eventData);

            CompletableFuture<SendResult<String, Envelope<?>>> future = 
                    kafkaTemplate.send(event.getTopic(), event.getKey(), envelope);
            
            future.whenComplete((result, throwable) -> {
                if (throwable == null) {
                    outboxService.markEventAsPublished(event.getId());
                    log.info("Outbox 이벤트 발행 성공: eventId={}, topic={}, partition={}, offset={}", 
                            event.getId(), event.getTopic(), 
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                } else {
                    outboxService.markEventAsFailed(event.getId(), throwable.getMessage());
                    log.error("Outbox 이벤트 발행 실패: eventId={}, error={}", 
                            event.getId(), throwable.getMessage(), throwable);
                }
            });
            
        } catch (Exception e) {
            outboxService.markEventAsFailed(event.getId(), e.getMessage());
            throw new RuntimeException("Outbox 이벤트 발행 실패", e);
        }
    }
}
