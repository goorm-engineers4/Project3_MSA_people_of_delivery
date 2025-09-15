package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class WeatherMenuResponseDTO {
   private String recommendedMenu;
   private String restaurantName;
   private String restaurantAddress;
   private List<String> alternativeMenus;
   private List<String> alternativeRestaurants;
   private String reasoning;
   private String weatherAdvice;
   private String todayWeatherSummary;
   private Boolean success;
   private String errorMessage;
   private String weatherDescKo;
}