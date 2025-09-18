package com.example.cloudfour.analyticsservice.batch.service;

import com.example.cloudfour.analyticsservice.batch.entity.SlaDailyAggregate;
import com.example.cloudfour.analyticsservice.batch.repo.SlaDailyAggregateRepository;
import com.example.cloudfour.analyticsservice.model.SlaMetric;
import com.example.cloudfour.analyticsservice.model.SlaMetricKind;
import com.example.cloudfour.analyticsservice.model.SlaStage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndTimestamp;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlaDailyAggregator {

    private final KafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;
    private final SlaDailyAggregateRepository repository;

    @Value("${kafka.topics.slaDurations:analytics.sla-durations.v1}")
    private String slaDurationsTopic;

    public void aggregateAndUpsert(LocalDate day) {
        Instant from = day.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        Map<SlaStage, List<Long>> byStage = new EnumMap<>(SlaStage.class);
        for (SlaStage s : SlaStage.values()) byStage.put(s, new ArrayList<>());

        Properties props = new Properties();
        props.putAll(kafkaProperties.buildConsumerProperties(null));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "sla-daily-batch");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        try (KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
            List<TopicPartition> partitions = consumer.partitionsFor(slaDurationsTopic).stream()
                    .map(info -> new TopicPartition(info.topic(), info.partition()))
                    .collect(Collectors.toList());
            consumer.assign(partitions);

            Map<TopicPartition, Long> timestamps = new HashMap<>();
            for (TopicPartition tp : partitions) timestamps.put(tp, from.toEpochMilli());
            Map<TopicPartition, OffsetAndTimestamp> offsets = consumer.offsetsForTimes(timestamps);
            for (TopicPartition tp : partitions) {
                OffsetAndTimestamp o = offsets.get(tp);
                if (o != null && o.offset() >= 0) consumer.seek(tp, o.offset());
                else consumer.seekToBeginning(Collections.singleton(tp));
            }

            long toMs = to.toEpochMilli();
            int emptyPolls = 0;
            while (true) {
                var records = consumer.poll(java.time.Duration.ofSeconds(1));
                    if (records.isEmpty()) {
                        emptyPolls++;
                    if (emptyPolls > 5) break;
                        continue;
                    }
                emptyPolls = 0;
                for (ConsumerRecord<String, byte[]> rec : records) {
                    long ts = rec.timestamp();
                    if (ts >= toMs) {
                        emptyPolls = 6;
                        continue;
                    }
                    try {
                        SlaMetric metric = objectMapper.readValue(rec.value(), SlaMetric.class);
                        if (metric.getKind() != SlaMetricKind.DURATION) continue;
                        if (metric.getDurationMs() == null || metric.getDurationMs() < 0) continue;
                        SlaStage stage = metric.getStage();
                        if (stage != null) byStage.get(stage).add(metric.getDurationMs());
                    } catch (Exception e) {
                        log.warn("SLA duration 레코드 파싱 실패: partition={}, offset={}, error={}", rec.partition(), rec.offset(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Kafka 소비 중 오류: {}", e.getMessage(), e);
        }

        for (Map.Entry<SlaStage, List<Long>> entry : byStage.entrySet()) {
            List<Long> values = entry.getValue();
            if (values.isEmpty()) continue;
            values.sort(Long::compareTo);
            long count = values.size();
            double avg = values.stream().mapToLong(Long::longValue).average().orElse(0);
            double p50 = percentile(values, 0.50);
            double p95 = percentile(values, 0.95);
            SlaDailyAggregate agg = SlaDailyAggregate.of(day, entry.getKey().name(), count, avg, p50, p95);
            repository.save(agg);
        }
        log.info("SLA 일일 스냅샷 저장 완료: day={}, stages={}", day, byStage.keySet());
    }

    private double percentile(List<Long> sorted, double q) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil(q * sorted.size()) - 1;
        idx = Math.max(0, Math.min(idx, sorted.size() - 1));
        return sorted.get(idx);
    }
}
