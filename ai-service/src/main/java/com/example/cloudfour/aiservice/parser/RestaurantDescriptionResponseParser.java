package com.example.cloudfour.aiservice.parser;

import com.example.cloudfour.aiservice.converter.RestaurantDescriptionConverter;
import com.example.cloudfour.aiservice.dto.GeminiResponseDTO;
import com.example.cloudfour.aiservice.dto.RestaurantDescriptionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RestaurantDescriptionResponseParser {

    public RestaurantDescriptionResponseDTO parseGeminiResponse(GeminiResponseDTO geminiResponse) {
        if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
            return RestaurantDescriptionConverter.toErrorResponse("AI 응답이 비어있습니다.");
        }
        
        String responseText = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
        String[] sections = responseText.split("\n\n");
        
        String description = "";
        String welcomeMessage = "";
        String atmosphereDescription = "";
        String recommendedDishes = "";
        String suggestedTags = "";
        
        for (String section : sections) {
            if (section.contains("가게 설명")) {
                description = section.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (section.contains("환영 메시지")) {
                welcomeMessage = section.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (section.contains("분위기 설명")) {
                atmosphereDescription = section.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (section.contains("추천 메뉴")) {
                recommendedDishes = section.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (section.contains("추천 태그")) {
                suggestedTags = section.replaceAll("^.*?[:.]\\s*", "").trim();
            }
        }
        
        return RestaurantDescriptionConverter.toSuccessResponse(
                description, 
                welcomeMessage, 
                atmosphereDescription, 
                recommendedDishes, 
                suggestedTags
        );
    }
}
