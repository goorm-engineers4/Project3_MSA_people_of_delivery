package com.example.cloudfour.modulecommon.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class OutboxMetrics {
    
    private final MeterRegistry meterRegistry;

    public void registerPendingEventsGauge(Supplier<Number> pendingCountSupplier) {
        Gauge.builder("outbox_pending_events", pendingCountSupplier)
                .register(meterRegistry);
    }

    public void registerAverageResidenceTimeGauge(Supplier<Number> averageTimeSupplier) {
        Gauge.builder("outbox_average_residence_time_seconds", averageTimeSupplier)
                .register(meterRegistry);
    }

    public void incrementEventPublished() {
        Counter.builder("outbox_event_published_total")
                .register(meterRegistry)
                .increment();
    }

    public void incrementEventPublishFailed() {
        Counter.builder("outbox_event_publish_failed_total")
                .register(meterRegistry)
                .increment();
    }

    public void incrementRetryCount() {
        Counter.builder("outbox_retry_count_total")
                .register(meterRegistry)
                .increment();
    }
}
