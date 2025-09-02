package com.example.cloudfour.aiservice.converter;

import com.example.cloudfour.aiservice.dto.RestaurantDescriptionResponseDTO;

public class RestaurantDescriptionConverter {
    
    public static RestaurantDescriptionResponseDTO toRestaurantDescriptionResponseDTO(boolean success, String errorMessage,
            String generatedDescription, String welcomeMessage, String atmosphereDescription, String recommendedDishes, String suggestedTags, String keyFeatures, String businessHighlights) {
        return RestaurantDescriptionResponseDTO.builder()
                .success(success)
                .errorMessage(errorMessage)
                .generatedDescription(generatedDescription)
                .welcomeMessage(welcomeMessage)
                .atmosphereDescription(atmosphereDescription)
                .recommendedDishes(recommendedDishes)
                .suggestedTags(suggestedTags)
                .keyFeatures(keyFeatures)
                .businessHighlights(businessHighlights)
                .build();
    }

    public static RestaurantDescriptionResponseDTO toSuccessResponse(String generatedDescription, 
            String welcomeMessage, String atmosphereDescription, String recommendedDishes, String suggestedTags) {
        return toRestaurantDescriptionResponseDTO(true, null, generatedDescription, welcomeMessage, 
                atmosphereDescription, recommendedDishes, suggestedTags, null, null);
    }

    public static RestaurantDescriptionResponseDTO toErrorResponse(String errorMessage) {
        return RestaurantDescriptionResponseDTO.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}