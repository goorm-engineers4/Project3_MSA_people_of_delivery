package com.example.cloudfour.aiservice.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RestaurantDescriptionResponseDTO {
    private boolean success;
    private String errorMessage;
    private String generatedDescription;
    private String welcomeMessage;
    private String atmosphereDescription;
    private String recommendedDishes;
    private String suggestedTags;
    private String keyFeatures;
    private String businessHighlights;
}