package com.example.cloudfour.aiservice.converter;

import com.example.cloudfour.aiservice.dto.WeatherMenuResponseDTO;

import java.util.List;

public class WeatherMenuConverter {
    
    public static WeatherMenuResponseDTO toWeatherMenuResponseDTO(String recommendedMenu, String restaurantName, 
            String restaurantAddress, String reasoning, List<String> alternativeMenus, 
            List<String> alternativeRestaurants, String weatherAdvice, String todayWeatherSummary, 
            Boolean success, String errorMessage, String weatherDescKo) {
        return WeatherMenuResponseDTO.builder()
                .recommendedMenu(recommendedMenu)
                .restaurantName(restaurantName)
                .restaurantAddress(restaurantAddress)
                .reasoning(reasoning)
                .alternativeMenus(alternativeMenus)
                .alternativeRestaurants(alternativeRestaurants)
                .weatherAdvice(weatherAdvice)
                .todayWeatherSummary(todayWeatherSummary)
                .success(success)
                .errorMessage(errorMessage)
                .weatherDescKo(weatherDescKo)
                .build();
    }

    public static WeatherMenuResponseDTO toSuccessResponse(String recommendedMenu, String restaurantName, 
            String restaurantAddress, String reasoning, List<String> alternativeMenus, 
            List<String> alternativeRestaurants, String weatherAdvice, String todayWeatherSummary) {
        return toWeatherMenuResponseDTO(recommendedMenu, restaurantName, restaurantAddress, reasoning, 
                alternativeMenus, alternativeRestaurants, weatherAdvice, todayWeatherSummary, 
                true, null, null);
    }

    public static WeatherMenuResponseDTO toErrorResponse(String errorMessage) {
        return WeatherMenuResponseDTO.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}