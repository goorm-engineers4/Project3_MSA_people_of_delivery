package com.example.cloudfour.analyticsservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlaMetric {
    private SlaMetricKind kind;
    private SlaStage stage;
    private String orderId;
    private String storeId;

    private Instant fromAt;
    private Instant toAt;

    private Long durationMs;
    private Long expectedWithinMs;
    private Long observedMs;
    private String reason;

    public static SlaMetric duration(SlaStage stage, String orderId, String storeId,
                                     Instant fromAt, Instant toAt, long durationMs) {
        SlaMetric m = new SlaMetric();
        m.kind = SlaMetricKind.DURATION;
        m.stage = stage;
        m.orderId = orderId;
        m.storeId = storeId;
        m.fromAt = fromAt;
        m.toAt = toAt;
        m.durationMs = durationMs;
        return m;
    }

    public static SlaMetric violation(SlaStage stage, String orderId, String storeId,
                                      Instant since, long expectedWithinMs, long observedMs, String reason) {
        SlaMetric m = new SlaMetric();
        m.kind = SlaMetricKind.VIOLATION;
        m.stage = stage;
        m.orderId = orderId;
        m.storeId = storeId;
        m.fromAt = since;
        m.expectedWithinMs = expectedWithinMs;
        m.observedMs = observedMs;
        m.reason = reason;
        return m;
    }

    public SlaMetricKind getKind() { return kind; }
    public SlaStage getStage() { return stage; }
    public String getOrderId() { return orderId; }
    public String getStoreId() { return storeId; }
    public Instant getFromAt() { return fromAt; }
    public Instant getToAt() { return toAt; }
    public Long getDurationMs() { return durationMs; }
    public Long getExpectedWithinMs() { return expectedWithinMs; }
    public Long getObservedMs() { return observedMs; }
    public String getReason() { return reason; }

    public void setKind(SlaMetricKind kind) { this.kind = kind; }
    public void setStage(SlaStage stage) { this.stage = stage; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public void setFromAt(Instant fromAt) { this.fromAt = fromAt; }
    public void setToAt(Instant toAt) { this.toAt = toAt; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public void setExpectedWithinMs(Long expectedWithinMs) { this.expectedWithinMs = expectedWithinMs; }
    public void setObservedMs(Long observedMs) { this.observedMs = observedMs; }
    public void setReason(String reason) { this.reason = reason; }
}
