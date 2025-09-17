package com.example.cloudfour.modulecommon.idempotency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageIdempotencyService {

    private final ProcessedMessageRepository repository;

    @Transactional
    public boolean markIfNotProcessed(String consumerName, String msgId, String topic) {
        if (consumerName == null || msgId == null) {
            return true;
        }
        final String normalizedTopic = (topic == null ? "unknown" : topic);
        try {
            if (repository.existsByConsumerNameAndTopicAndMsgId(consumerName, normalizedTopic, msgId)) {
                return false;
            }
            ProcessedMessage pm = ProcessedMessage.builder()
                    .consumerName(consumerName)
                    .msgId(msgId)
                    .topic(normalizedTopic)
                    .createdAt(Instant.now())
                    .build();
            repository.save(pm);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("멱등성 충돌: consumer={}, topic={}, msgId={}", consumerName, normalizedTopic, msgId);
            return false;
        }
    }
}
