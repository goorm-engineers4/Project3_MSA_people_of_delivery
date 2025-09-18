package com.example.cloudfour.analyticsservice.batch.config;

import com.example.cloudfour.analyticsservice.batch.service.SlaDailyAggregator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SlaDailyBatchConfig {

    private final SlaDailyAggregator aggregator;

    @Bean
    public Job slaDailySnapshotJob(JobRepository jobRepository, Step slaDailySnapshotStep) {
        return new JobBuilder("slaDailySnapshotJob", jobRepository)
                .start(slaDailySnapshotStep)
                .build();
    }

    @Bean
    public Step slaDailySnapshotStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("slaDailySnapshotStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate day = LocalDate.now().minusDays(1);
                    log.info("SLA 일일 스냅샷 배치 시작: day={}", day);
                    aggregator.aggregateAndUpsert(day);
                    log.info("SLA 일일 스냅샷 배치 완료: day={}", day);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void scheduleDaily() {
        try {
            LocalDate day = LocalDate.now().minusDays(1);
            log.info("[Scheduler] SLA 일일 스냅샷 실행 트리거: day={}", day);
            aggregator.aggregateAndUpsert(day);
        } catch (Exception e) {
            log.error("[Scheduler] SLA 일일 스냅샷 실패: {}", e.getMessage(), e);
        }
    }
}
