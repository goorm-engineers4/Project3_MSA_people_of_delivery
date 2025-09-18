package com.example.cloudfour.modulecommon.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class KafkaMetrics {
    
    private final MeterRegistry meterRegistry;

    public void recordProcessingLatency(String topic, String groupId, Duration duration) {
        Timer.builder("kafka_processing_latency_ms")
                .tag("topic", topic)
                .tag("group_id", groupId)
                .register(meterRegistry)
                .record(duration);
    }

    public void incrementMessageProcessed(String topic, String groupId) {
        Counter.builder("kafka_message_processed_total")
                .tag("topic", topic)
                .tag("group_id", groupId)
                .tag("status", "success")
                .register(meterRegistry)
                .increment();
    }

    public void incrementMessageProcessingFailed(String topic, String groupId) {
        Counter.builder("kafka_message_processed_total")
                .tag("topic", topic)
                .tag("group_id", groupId)
                .tag("status", "failed")
                .register(meterRegistry)
                .increment();
    }

    public void incrementRetryCount(String topic, String groupId) {
        Counter.builder("kafka_retry_count_total")
                .tag("topic", topic)
                .tag("group_id", groupId)
                .register(meterRegistry)
                .increment();
    }

    public void incrementDltCount(String topic, String groupId) {
        Counter.builder("kafka_dlt_count_total")
                .tag("topic", topic)
                .tag("group_id", groupId)
                .register(meterRegistry)
                .increment();
    }
}


