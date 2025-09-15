package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherMenuRequestDTO {
    private Long userId;
    private String city;
    private String weather;
}