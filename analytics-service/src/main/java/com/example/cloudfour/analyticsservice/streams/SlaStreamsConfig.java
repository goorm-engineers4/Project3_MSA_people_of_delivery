package com.example.cloudfour.analyticsservice.streams;

import com.example.cloudfour.analyticsservice.config.SlaThresholds;
import com.example.cloudfour.analyticsservice.model.*;
import com.example.cloudfour.modulecommon.messaging.Envelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.StreamsBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
public class SlaStreamsConfig {

    private final ObjectMapper objectMapper;
    private final SlaThresholds thresholds;

    @Value("${kafka.topics.orderEvents:order.events.v1}")
    private String orderEventsTopic;
    @Value("${kafka.topics.inventoryEvents:inventory.events.v1}")
    private String inventoryEventsTopic;
    @Value("${kafka.topics.paymentEvents:payment.events.v1}")
    private String paymentEventsTopic;

    @Value("${kafka.topics.slaDurations:analytics.sla-durations.v1}")
    private String slaDurationsTopic;
    @Value("${kafka.topics.slaViolations:analytics.sla-violations.v1}")
    private String slaViolationsTopic;

    public SlaStreamsConfig(ObjectMapper objectMapper, SlaThresholds thresholds) {
        this.objectMapper = objectMapper;
        this.thresholds = thresholds;
    }

    @Bean
    public KStream<String, Envelope<Object>> slaTopology(StreamsBuilder builder) {
        var stringSerde = Serdes.String();
        var envSerde = new JsonSerde<>(Envelope.class, objectMapper);
        var metricSerde = new JsonSerde<>(SlaMetric.class, objectMapper);
        var stateSerde = new JsonSerde<>(SlaState.class, objectMapper);

        KStream<String, Envelope<Object>> orders = builder.stream(orderEventsTopic, Consumed.with(stringSerde, envSerde));
        KStream<String, Envelope<Object>> inv = builder.stream(inventoryEventsTopic, Consumed.with(stringSerde, envSerde));
        KStream<String, Envelope<Object>> pay = builder.stream(paymentEventsTopic, Consumed.with(stringSerde, envSerde));

        KStream<String, Envelope<Object>> merged = orders.merge(inv).merge(pay);

        var storeBuilder = Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore("sla-tracker-store"),
                stringSerde, stateSerde
        );
        builder.addStateStore(storeBuilder);

        KStream<String, SlaMetric> metrics = merged.flatTransform(
                () -> new SlaTrackerTransformer(thresholds, objectMapper),
                Named.as("sla-tracker"),
                "sla-tracker-store"
        );

        var branches = metrics.split(Named.as("sla-"))
                .branch((k, m) -> m.getKind() == SlaMetricKind.DURATION, Branched.as("dur"))
                .branch((k, m) -> m.getKind() == SlaMetricKind.VIOLATION, Branched.as("vio"));

        branches.get("sla-dur").to(slaDurationsTopic, Produced.with(stringSerde, metricSerde));
        branches.get("sla-vio").to(slaViolationsTopic, Produced.with(stringSerde, metricSerde));

        return merged;
    }
}

