package com.example.cloudfour.analyticsservice.batch.repo;

import com.example.cloudfour.analyticsservice.batch.entity.SlaDailyAggregate;
import com.example.cloudfour.analyticsservice.batch.entity.SlaDailyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaDailyAggregateRepository extends JpaRepository<SlaDailyAggregate, SlaDailyId> {
}

