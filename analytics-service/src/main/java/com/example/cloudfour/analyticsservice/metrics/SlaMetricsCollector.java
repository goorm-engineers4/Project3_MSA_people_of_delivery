package com.example.cloudfour.analyticsservice.metrics;

import com.example.cloudfour.analyticsservice.model.SlaMetric;
import com.example.cloudfour.analyticsservice.model.SlaMetricKind;
import com.example.cloudfour.analyticsservice.model.SlaStage;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Map<SlaStage, Timer> timers = new EnumMap<>(SlaStage.class);

    private Timer timerFor(SlaStage stage) {
        return timers.computeIfAbsent(stage, s -> Timer.builder("sla_stage_duration_ms")
                .description("SLA stage duration in milliseconds")
                .publishPercentileHistogram(true)
                .percentiles(0.5, 0.9, 0.95, 0.99)
                .tag("stage", s.name())
                .register(meterRegistry));
    }

    @KafkaListener(
            topics = "${kafka.topics.slaDurations:analytics.sla-durations.v1}",
            groupId = "sla-metrics-collector",
            containerFactory = "slaMetricKafkaListenerContainerFactory"
    )
    public void onDuration(SlaMetric metric,
                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                           @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            if (metric == null || metric.getKind() != SlaMetricKind.DURATION) return;
            if (metric.getDurationMs() == null || metric.getDurationMs() < 0) return;
            timerFor(metric.getStage()).record(Duration.ofMillis(metric.getDurationMs()));
        } catch (Exception e) {
            log.error("SLA duration 메트릭 수집 실패: {}", e.getMessage(), e);
        }
    }
}

