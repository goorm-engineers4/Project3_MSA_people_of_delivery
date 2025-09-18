package com.example.cloudfour.analyticsservice.batch.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sla_daily_aggregates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SlaDailyAggregate {

    @EmbeddedId
    private SlaDailyId id;

    @Column(nullable = false)
    private long count;

    @Column(nullable = false)
    private double avgMs;

    @Column(nullable = false)
    private double p50Ms;

    @Column(nullable = false)
    private double p95Ms;

    public static SlaDailyAggregate of(LocalDate day, String stage, long count, double avgMs, double p50Ms, double p95Ms) {
        return SlaDailyAggregate.builder()
                .id(new SlaDailyId(day, stage))
                .count(count)
                .avgMs(avgMs)
                .p50Ms(p50Ms)
                .p95Ms(p95Ms)
                .build();
    }
}

