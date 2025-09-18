package com.example.cloudfour.analyticsservice.batch;

import com.example.cloudfour.analyticsservice.batch.service.SlaDailyAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.sla.batch", name = "runOnStartup", havingValue = "true", matchIfMissing = false)
public class LocalSlaBatchRunner implements CommandLineRunner {

    private final SlaDailyAggregator aggregator;

    @Override
    public void run(String... args) throws Exception {
        LocalDate today = LocalDate.now();
        log.info("[Local] SLA 일일 스냅샷 즉시 실행: day={}", today);
        aggregator.aggregateAndUpsert(today);
    }
}

