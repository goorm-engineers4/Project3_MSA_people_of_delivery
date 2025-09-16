package com.example.cloudfour.modulecommon.messaging;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.example.cloudfour.modulecommon.monitoring.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.kafka.support.Acknowledgment;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.dlq.monitor", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class DLQMonitor {
    
    private final NotificationService notificationService;
    private final AtomicLong dlqMessageCount = new AtomicLong(0);
    private final AtomicLong lastAlertTime = new AtomicLong(0);

    @Value("${app.dlq.monitor.alert-interval-ms:300000}")
    private long alertIntervalMs;

    @Value("${app.dlq.monitor.alert-threshold:10}")
    private int alertThreshold;

    @KafkaListener(
        topics = {
            "${kafka.topics.orderEventsDLQ:order.events.dlq.v1}",
            "${kafka.topics.orderCommandsDLQ:order.commands.dlq.v1}",
            "${kafka.topics.inventoryEventsDLQ:inventory.events.dlq.v1}",
            "${kafka.topics.inventoryCommandsDLQ:inventory.commands.dlq.v1}",
            "${kafka.topics.paymentEventsDLQ:payment.events.dlq.v1}",
            "${kafka.topics.paymentCommandsDLQ:payment.commands.dlq.v1}"
        },
        groupId = "dlq-monitor",
        containerFactory = "dlqKafkaListenerContainerFactory"
    )
    public void monitorDLQMessage(
            @Payload Object payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(value = "x-original-delivery-attempt", required = false) byte[] originalAttempt,
            Acknowledgment acknowledgment) {
        
        log.info("DLQMonitor 메시지 수신: topic={}, key={}, payloadType={}, offset={}", 
                topic, key, payload != null ? payload.getClass().getSimpleName() : "null", offset);
        
        try {
            long count = dlqMessageCount.incrementAndGet();

            if (payload instanceof DLQMessage) {
                DLQMessage dlqMessage = (DLQMessage) payload;
                int attempt = parseAttempt(originalAttempt);
                log.warn("DLQ 메시지 감지: topic={}, key={}, originalTopic={}, retryCount(원본 시도 수)={}, " +
                        "errorClass={}, totalDLQCount={}", 
                        topic, key, dlqMessage.getOriginalTopic(), attempt,
                        dlqMessage.getErrorClass(), count);

                if (count >= alertThreshold) {
                    sendAlertIfNeeded(topic, dlqMessage, count, attempt);
                }

                recordMetrics(topic, dlqMessage);
            } else {
                log.warn("DLQ 원본 메시지 감지: topic={}, key={}, payloadType={}, totalDLQCount={}", 
                        topic, key, payload.getClass().getSimpleName(), count);

                DLQMessage dlqMessage = DLQMessage.builder()
                        .originalTopic(topic.replace(".dlq.v1", ".v1"))
                        .originalKey(key)
                        .originalMessage(null)
                        .errorMessage("SerializationException 또는 기타 역직렬화 오류")
                        .errorClass("SerializationException")
                        .retryCount(0)
                        .failedAt(java.time.LocalDateTime.now())
                        .metadata(java.util.Map.of("payloadType", payload.getClass().getSimpleName()))
                        .build();

                int attempt = parseAttempt(originalAttempt);
                if (count >= alertThreshold) {
                    sendAlertIfNeeded(topic, dlqMessage, count, attempt);
                }

                recordMetrics(topic, dlqMessage);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("DLQ 모니터링 중 오류 발생: topic={}, key={}, error={}", 
                    topic, key, e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }

    private void sendAlertIfNeeded(String topic, DLQMessage dlqMessage, long count, int attempt) {
        long currentTime = System.currentTimeMillis();
        long lastAlert = lastAlertTime.get();
        
        // 마지막 알림 후 일정 시간이 지났는지 확인
        if (currentTime - lastAlert > alertIntervalMs) {
            if (lastAlertTime.compareAndSet(lastAlert, currentTime)) {
                sendAlert(topic, dlqMessage, count, attempt);
            }
        }
    }

    private void sendAlert(String topic, DLQMessage dlqMessage, long count, int attempt) {
        try {
            String service = inferService(dlqMessage.getOriginalTopic());
            String alertMessage = String.format(
                "🚨 DLQ 알림\n" +
                "토픽: %s (%s)\n" +
                "원본 토픽: %s\n" +
                "키: %s\n" +
                "재시도 횟수(원본): %d\n" +
                "에러 클래스: %s\n" +
                "에러 메시지: %s\n" +
                "실패 시점: %s\n" +
                "총 DLQ 메시지 수: %d",
                topic, service,
                dlqMessage.getOriginalTopic(),
                dlqMessage.getOriginalKey(),
                attempt,
                dlqMessage.getErrorClass(),
                dlqMessage.getErrorMessage(),
                dlqMessage.getFailedAt(),
                count
            );
            
            log.error("DLQ 알림 발송: {}", alertMessage);
            log.info("NotificationService 상태: {}", notificationService != null ? "사용 가능" : "null");
            if (notificationService != null) {
                notificationService.sendAlert("DLQ Alert", alertMessage);
                log.info("Discord 알림 전송 완료");
            } else {
                log.warn("NotificationService가 null이므로 알림을 전송할 수 없습니다");
            }
            
        } catch (Exception e) {
            log.error("알림 발송 실패: {}", e.getMessage(), e);
        }
    }

    private int parseAttempt(byte[] header) {
        try {
            if (header == null) return 0;
            return Integer.parseInt(new String(header, java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) { return 0; }
    }

    private String inferService(String originalTopic) {
        if (originalTopic == null) return "알 수 없음";
        if (originalTopic.startsWith("inventory.")) return "store-service";
        if (originalTopic.startsWith("payment.")) return "payment-service";
        if (originalTopic.startsWith("order.")) return "cart-service"; // 오케스트레이터 경로
        return "알 수 없음";
    }
    

    private void recordMetrics(String topic, DLQMessage dlqMessage) {
        try {

            log.info("DLQ 메트릭: topic={}, originalTopic={}, errorClass={}, retryCount={}", 
                    topic, dlqMessage.getOriginalTopic(), dlqMessage.getErrorClass(), 
                    dlqMessage.getRetryCount());
            
        } catch (Exception e) {
            log.error("메트릭 수집 실패: {}", e.getMessage(), e);
        }
    }

    public long getDLQMessageCount() {
        return dlqMessageCount.get();
    }

    public void resetDLQMessageCount() {
        dlqMessageCount.set(0);
        log.info("DLQ 메시지 수가 리셋되었습니다.");
    }
}
