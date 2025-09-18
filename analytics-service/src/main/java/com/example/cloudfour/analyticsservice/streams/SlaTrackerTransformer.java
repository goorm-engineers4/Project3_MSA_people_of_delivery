package com.example.cloudfour.analyticsservice.streams;

import com.example.cloudfour.analyticsservice.config.SlaThresholds;
import com.example.cloudfour.analyticsservice.model.*;
import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.Punctuator;
import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Instant;
import java.util.*;

public class SlaTrackerTransformer implements Transformer<String, Envelope<Object>, Iterable<KeyValue<String, SlaMetric>>> {

    private final SlaThresholds thresholds;
    private final ObjectMapper objectMapper;
    private ProcessorContext context;
    private KeyValueStore<String, SlaState> store;

    public SlaTrackerTransformer(SlaThresholds thresholds, ObjectMapper objectMapper) {
        this.thresholds = thresholds;
        this.objectMapper = objectMapper;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;
        this.store = context.getStateStore("sla-tracker-store");

        context.schedule(java.time.Duration.ofMinutes(1), PunctuationType.WALL_CLOCK_TIME, (Punctuator) timestamp -> {
            List<KeyValue<String, SlaMetric>> out = new ArrayList<>();
            try (KeyValueIterator<String, SlaState> it = store.all()) {
                while (it.hasNext()) {
                    var next = it.next();
                    String orderId = next.key;
                    SlaState s = next.value;
                    if (s == null) continue;
                    checkTimeouts(orderId, s, out, timestamp);
                    if (isTerminal(s)) {
                        store.delete(orderId);
                    }
                }
            }
            for (var kv : out) {
                context.forward(kv.key, kv.value);
            }
        });
    }

    @Override
    public Iterable<KeyValue<String, SlaMetric>> transform(String orderId, Envelope<Object> env) {
        List<KeyValue<String, SlaMetric>> out = new ArrayList<>();
        if (env == null || env.getMeta() == null) return out;
        String type = env.getMeta().getType();
        Map<String, Object> payload = castToMap(env.getPayload());

        SlaState state = Optional.ofNullable(store.get(orderId)).orElse(new SlaState());
        state.lastUpdatedAt = Instant.ofEpochMilli(context.currentSystemTimeMs());

        String storeId = stringOrNull(payload.get("storeId"));
        if (storeId != null) state.storeId = storeId;

        Instant eventTime = extractInstant(payload, type);
        if (eventTime == null) {
            eventTime = Instant.ofEpochMilli(context.timestamp());
        }

        switch (type) {
            case "OrderCreated":
                state.createdAt = coalesce(state.createdAt, eventTime);
                break;
            case "InventoryReserved":
                state.reservedAt = coalesce(state.reservedAt, eventTime);
                break;
            case "PaymentAuthorized":
                state.authorizedAt = coalesce(state.authorizedAt, eventTime);
                break;
            case "InventoryCommitted":
                state.committedAt = coalesce(state.committedAt, eventTime);
                break;
            case "InventoryReleased":
                break;
            case "InventoryReleaseCompleted":
                state.releasedAt = coalesce(state.releasedAt, eventTime);
                break;
            case "InventoryReservationFailed":
                state.reservationFailedAt = coalesce(state.reservationFailedAt, eventTime);
                break;
            case "PaymentFailed":
                state.paymentFailedAt = coalesce(state.paymentFailedAt, eventTime);
                break;
            case "OrderCanceled":
                state.canceledAt = coalesce(state.canceledAt, eventTime);
                break;
            default:
                break;
        }

        if (state.createdAt != null && state.reservedAt != null && !state.emittedCreatedToReserved) {
            long d = java.time.Duration.between(state.createdAt, state.reservedAt).toMillis();
            out.add(KeyValue.pair(orderId, SlaMetric.duration(SlaStage.CREATED_TO_RESERVED, orderId, state.storeId, state.createdAt, state.reservedAt, d)));
            state.emittedCreatedToReserved = true;
        }
        if (state.reservedAt != null && state.authorizedAt != null && !state.emittedReservedToAuthorized) {
            long d = java.time.Duration.between(state.reservedAt, state.authorizedAt).toMillis();
            out.add(KeyValue.pair(orderId, SlaMetric.duration(SlaStage.RESERVED_TO_AUTHORIZED, orderId, state.storeId, state.reservedAt, state.authorizedAt, d)));
            state.emittedReservedToAuthorized = true;
        }
        if (state.authorizedAt != null && state.committedAt != null && !state.emittedAuthorizedToCommitted) {
            long d = java.time.Duration.between(state.authorizedAt, state.committedAt).toMillis();
            out.add(KeyValue.pair(orderId, SlaMetric.duration(SlaStage.AUTHORIZED_TO_COMMITTED, orderId, state.storeId, state.authorizedAt, state.committedAt, d)));
            state.emittedAuthorizedToCommitted = true;
        }
        if (state.reservedAt != null && (state.releasedAt != null || state.reservationFailedAt != null || state.paymentFailedAt != null) && !state.emittedReservedToReleased) {
            Instant to = firstNonNull(state.releasedAt, state.reservationFailedAt, state.paymentFailedAt);
            long d = java.time.Duration.between(state.reservedAt, to).toMillis();
            out.add(KeyValue.pair(orderId, SlaMetric.duration(SlaStage.RESERVED_TO_RELEASED, orderId, state.storeId, state.reservedAt, to, d)));
            state.emittedReservedToReleased = true;
        }

        store.put(orderId, state);
        return out;
    }

    private void checkTimeouts(String orderId, SlaState s, List<KeyValue<String, SlaMetric>> out, long nowMs) {
        if (s.createdAt != null && s.reservedAt == null && !s.violatedCreatedToReserved) {
            long observed = nowMs - s.createdAt.toEpochMilli();
            if (observed > thresholds.getCreatedToReservedMs()) {
                out.add(KeyValue.pair(orderId, SlaMetric.violation(
                        SlaStage.CREATED_TO_RESERVED, orderId, s.storeId, s.createdAt,
                        thresholds.getCreatedToReservedMs(), observed, "RESERVED timeout")));
                s.violatedCreatedToReserved = true;
            }
        }
        if (s.reservedAt != null && s.authorizedAt == null && !s.violatedReservedToAuthorized) {
            long observed = nowMs - s.reservedAt.toEpochMilli();
            if (observed > thresholds.getReservedToAuthorizedMs()) {
                out.add(KeyValue.pair(orderId, SlaMetric.violation(
                        SlaStage.RESERVED_TO_AUTHORIZED, orderId, s.storeId, s.reservedAt,
                        thresholds.getReservedToAuthorizedMs(), observed, "AUTHORIZED timeout")));
                s.violatedReservedToAuthorized = true;
            }
        }
        if (s.authorizedAt != null && s.committedAt == null && !s.violatedAuthorizedToCommitted) {
            long observed = nowMs - s.authorizedAt.toEpochMilli();
            if (observed > thresholds.getAuthorizedToCommittedMs()) {
                out.add(KeyValue.pair(orderId, SlaMetric.violation(
                        SlaStage.AUTHORIZED_TO_COMMITTED, orderId, s.storeId, s.authorizedAt,
                        thresholds.getAuthorizedToCommittedMs(), observed, "COMMITTED timeout")));
                s.violatedAuthorizedToCommitted = true;
            }
        }
        if (s.reservedAt != null && s.releasedAt == null && s.reservationFailedAt == null && s.paymentFailedAt == null && s.committedAt == null && !s.violatedReservedToReleased) {
            long observed = nowMs - s.reservedAt.toEpochMilli();
            if (observed > thresholds.getReservedToReleasedMs()) {
                out.add(KeyValue.pair(orderId, SlaMetric.violation(
                        SlaStage.RESERVED_TO_RELEASED, orderId, s.storeId, s.reservedAt,
                        thresholds.getReservedToReleasedMs(), observed, "RESERVATION hold timeout")));
                s.violatedReservedToReleased = true;
            }
        }
    }

    private boolean isTerminal(SlaState s) {
        return s.committedAt != null || s.releasedAt != null || s.reservationFailedAt != null || s.paymentFailedAt != null || s.canceledAt != null;
    }

    private Map<String, Object> castToMap(Object payload) {
        if (payload == null) return Collections.emptyMap();
        if (payload instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) map;
            return m;
        }
        return objectMapper.convertValue(payload, Map.class);
    }

    private Instant extractInstant(Map<String, Object> payload, String type) {
        String[] candidates = switch (type) {
            case "OrderCreated" -> new String[]{"createdAt"};
            case "InventoryReserved" -> new String[]{"reservedAt"};
            case "PaymentAuthorized" -> new String[]{"authorizedAt"};
            case "InventoryCommitted" -> new String[]{"committedAt"};
            case "InventoryReleased" -> new String[]{"releasedAt"};
            case "InventoryReservationFailed", "PaymentFailed" -> new String[]{"failedAt"};
            case "InventoryReleaseCompleted" -> new String[]{"completedAt"};
            case "OrderCanceled" -> new String[]{"canceledAt"};
            default -> new String[]{};
        };
        for (String k : candidates) {
            Instant v = instantOrNull(payload.get(k));
            if (v != null) return v;
        }
        return null;
    }

    private Instant instantOrNull(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof Long l) return Instant.ofEpochMilli(l);
            if (v instanceof Integer i) return Instant.ofEpochMilli(i.longValue());
            if (v instanceof String s) return Instant.parse(s);
        } catch (Exception ignored) { }
        return null;
    }

    private String stringOrNull(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Instant coalesce(Instant a, Instant b) {
        return a != null ? a : b;
    }

    private Instant firstNonNull(Instant... instants) {
        for (Instant i : instants) if (i != null) return i;
        return null;
    }
}
