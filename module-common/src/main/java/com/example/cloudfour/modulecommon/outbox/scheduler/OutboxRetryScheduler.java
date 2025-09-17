package com.example.cloudfour.modulecommon.outbox.scheduler;

import com.example.cloudfour.modulecommon.monitoring.OutboxMetrics;
import com.example.cloudfour.modulecommon.outbox.entity.OutboxEvent;
import com.example.cloudfour.modulecommon.outbox.repository.OutboxEventRepository;
import com.example.cloudfour.modulecommon.outbox.event.OutboxEventReadyToPublish;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true", matchIfMissing = false)
public class OutboxRetryScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxMetrics outboxMetrics;

    private static final Duration FAILED_RETRY_THRESHOLD = Duration.ofMinutes(1);

    @Scheduled(fixedDelayString = "${outbox.retry.fixedDelayMs:30000}")
    public void retryPendingAndFailed() {
        try {
            List<OutboxEvent> pendings = outboxEventRepository.findPendingEvents();
            List<OutboxEvent> oldFailed = outboxEventRepository.findFailedEventsBefore(Instant.now().minus(FAILED_RETRY_THRESHOLD));

            int total = pendings.size() + oldFailed.size();
            if (total == 0) return;

            for (OutboxEvent e : pendings) publish(e);
            for (OutboxEvent e : oldFailed) publish(e);
        } catch (Exception e) {
            log.error("Outbox 재시도 작업 실패: {}", e.getMessage(), e);
        }
    }

    private void publish(OutboxEvent e) {
        try {
            OutboxEventReadyToPublish evt = new OutboxEventReadyToPublish(
                    e.getId(), e.getAggregateId(), e.getAggregateType(), e.getEventType(),
                    e.getEventData(), e.getTopic(), e.getKey()
            );
            eventPublisher.publishEvent(evt);
            outboxMetrics.incrementRetryCount();
        } catch (Exception ex) {
            log.error("Outbox 재발행 실패: id={}, error={}", e.getId(), ex.getMessage(), ex);
        }
    }
}

