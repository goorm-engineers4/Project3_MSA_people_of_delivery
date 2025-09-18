package com.example.cloudfour.analyticsservice.consumer;

import com.example.cloudfour.analyticsservice.model.SlaMetric;
import com.example.cloudfour.analyticsservice.model.SlaMetricKind;
import com.example.cloudfour.analyticsservice.model.SlaStage;
import com.example.cloudfour.modulecommon.monitoring.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlaViolationAlertListener {

    private final java.util.Optional<NotificationService> notificationService;

    @KafkaListener(
            topics = "${kafka.topics.slaViolations:analytics.sla-violations.v1}",
            groupId = "sla-violation-alerts",
            containerFactory = "slaMetricKafkaListenerContainerFactory"
    )
    public void onViolation(SlaMetric metric,
                            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                            @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            if (metric == null || metric.getKind() != SlaMetricKind.VIOLATION) return;
            String title = "SLA 위반 감지: " + metric.getStage();
            String content = String.format(
                    "orderId=%s, stage=%s, storeId=%s, since=%s, expected<=%dms, observed=%dms",
                    metric.getOrderId(), metric.getStage(), metric.getStoreId(),
                    metric.getFromAt(), metric.getExpectedWithinMs(), metric.getObservedMs());
            log.warn("{}", content);
            notificationService.ifPresent(svc -> svc.sendAlert(title, content));
        } catch (Exception e) {
            log.error("SLA 위반 알림 처리 실패: {}", e.getMessage(), e);
        }
    }
}
