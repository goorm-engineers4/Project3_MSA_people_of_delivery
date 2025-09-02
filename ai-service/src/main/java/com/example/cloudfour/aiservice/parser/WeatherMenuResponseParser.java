package com.example.cloudfour.aiservice.parser;

import com.example.cloudfour.aiservice.converter.WeatherMenuConverter;
import com.example.cloudfour.aiservice.dto.GeminiResponseDTO;
import com.example.cloudfour.aiservice.dto.WeatherMenuResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class WeatherMenuResponseParser {

    public WeatherMenuResponseDTO parseGeminiResponse(GeminiResponseDTO geminiResponse) {
        if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
            return WeatherMenuConverter.toErrorResponse("AI 응답이 비어있습니다.");
        }
        
        String responseText = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
        String[] lines = responseText.split("\n");
        
        String recommendedMenu = "";
        String restaurantName = "";
        String restaurantAddress = "";
        String reasoning = "";
        String alternativeMenus = "";
        String alternativeRestaurants = "";
        String weatherAdvice = "";
        String todayWeatherSummary = "";
        
        for (String line : lines) {
            if (line.contains("추천 메뉴:")) {
                recommendedMenu = line.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (line.contains("추천 가게:")) {
                restaurantName = line.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (line.contains("가게 주소:")) {
                restaurantAddress = line.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (line.contains("추천 이유:")) {
                reasoning = line.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (line.contains("대안 메뉴:")) {
                alternativeMenus = line.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (line.contains("대안 가게:")) {
                alternativeRestaurants = line.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (line.contains("날씨 조언:")) {
                weatherAdvice = line.replaceAll("^.*?[:.]\\s*", "").trim();
            } else if (line.contains("오늘 날씨 요약:")) {
                todayWeatherSummary = line.replaceAll("^.*?[:.]\\s*", "").trim();
            }
        }
        
        List<String> alternativeMenuList = Arrays.asList(alternativeMenus.split(","));
        List<String> alternativeRestaurantList = Arrays.asList(alternativeRestaurants.split(","));
        
        log.info("AI 응답 파싱 결과 - recommendedMenu: '{}', restaurantName: '{}'", 
                recommendedMenu, restaurantName);
        
        return WeatherMenuConverter.toSuccessResponse(
                recommendedMenu, 
                restaurantName, 
                restaurantAddress, 
                reasoning, 
                alternativeMenuList, 
                alternativeRestaurantList, 
                weatherAdvice, 
                todayWeatherSummary
        );
    }
}
