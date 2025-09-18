package com.example.cloudfour.analyticsservice.batch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SlaDailyId implements Serializable {
    @Column(name = "day", nullable = false)
    private LocalDate day;

    @Column(name = "stage", nullable = false, length = 64)
    private String stage;
}

