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
        try {
            if (repository.existsByConsumerNameAndMsgId(consumerName, msgId)) {
                return false;
            }
            ProcessedMessage pm = ProcessedMessage.builder()
                    .consumerName(consumerName)
                    .msgId(msgId)
                    .topic(topic == null ? "unknown" : topic)
                    .createdAt(Instant.now())
                    .build();
            repository.save(pm);
            return true;
        } catch (DataIntegrityViolationException e) {
            log.warn("멱등성 충돌: consumer={}, msgId={}", consumerName, msgId);
            return false;
        }
    }
}

