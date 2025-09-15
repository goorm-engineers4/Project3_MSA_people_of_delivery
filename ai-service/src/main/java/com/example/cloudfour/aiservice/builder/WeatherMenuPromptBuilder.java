package com.example.cloudfour.aiservice.builder;

import org.springframework.stereotype.Component;

@Component
public class WeatherMenuPromptBuilder {

    public String buildWeatherMenuPrompt(String city, String weather, String weatherDescKo, 
                                        String temp, Double feelsLike, String humidity, 
                                        Double windSpeed, Double rain1h) {
        StringBuilder prompt = new StringBuilder();
        appendIntroduction(prompt);
        appendBasicWeatherInfo(prompt, city, weather, weatherDescKo);
        appendDetailedWeatherInfo(prompt, temp, feelsLike, humidity, windSpeed, rain1h);
        appendResponseFormat(prompt);
        
        return prompt.toString();
    }
    
    private void appendIntroduction(StringBuilder prompt) {
        prompt.append("아래 정보를 참고하여 반드시 모든 항목을 빠짐없이 채워서, 손님이 읽기 좋게 자연스럽고 구체적으로 작성해 주세요.\n");
        prompt.append("정보가 부족하면 창의적으로 예시로 채워도 좋습니다.\n\n");
    }
    
    private void appendBasicWeatherInfo(StringBuilder prompt, String city, String weather, String weatherDescKo) {
        prompt.append("도시: ").append(city).append("\n");
        
        if (isNotEmpty(weather)) {
            prompt.append("날씨: ").append(weather).append("\n");
        }
        
        if (isValidWeatherDesc(weatherDescKo)) {
            prompt.append("한글 날씨 설명: ").append(weatherDescKo).append("\n");
            prompt.append("특히 '날씨 조언'에는 한글 날씨 설명(예: ").append(weatherDescKo)
                  .append(")을 참고하여, 오늘의 날씨에 맞는 조언을 자연스럽게 작성해 주세요.\n");
        }
    }
    
    private void appendDetailedWeatherInfo(StringBuilder prompt, String temp, Double feelsLike, 
                                         String humidity, Double windSpeed, Double rain1h) {
        if (hasDetailedWeatherInfo(temp, feelsLike, humidity, windSpeed, rain1h)) {
            prompt.append("상세 날씨 정보: ");
            
            appendTemperatureInfo(prompt, temp, feelsLike);
            appendAtmosphereInfo(prompt, humidity, windSpeed);
            appendPrecipitationInfo(prompt, rain1h);
            
            prompt.append("\n");
        }
    }
    
    private void appendTemperatureInfo(StringBuilder prompt, String temp, Double feelsLike) {
        if (temp != null) {
            prompt.append("현재 온도: ").append(temp).append("°C, ");
        }
        if (feelsLike != null) {
            prompt.append("체감 온도: ").append(feelsLike).append("°C, ");
        }
    }
    
    private void appendAtmosphereInfo(StringBuilder prompt, String humidity, Double windSpeed) {
        if (humidity != null) {
            prompt.append("습도: ").append(humidity).append("%, ");
        }
        if (windSpeed != null) {
            prompt.append("풍속: ").append(windSpeed).append("m/s, ");
        }
    }
    
    private void appendPrecipitationInfo(StringBuilder prompt, Double rain1h) {
        if (rain1h != null) {
            prompt.append("강수량(1시간): ").append(rain1h).append("mm, ");
        }
    }
    
    private void appendResponseFormat(StringBuilder prompt) {
        prompt.append("\n아래 형식에 맞춰 반드시 모든 항목을 채워서 응답해 주세요.\n");
        
        appendFormatExamples(prompt);
    }
    
    private void appendFormatExamples(StringBuilder prompt) {
        prompt.append("추천 메뉴: (예: 시원한 콩국수)\n");
        prompt.append("추천 가게: (예: 진주회관 (시청))\n");
        prompt.append("가게 주소: (예: 서울특별시 중구 세종대로 11길 20)\n");
        prompt.append("추천 이유: (예: 새콤달콤한 양념에 비벼 먹는 비빔국수는 입맛을 돋우는 데 제격입니다.)\n");
        prompt.append("대안 메뉴: (예: 비빔국수)\n");
        prompt.append("대안 가게: (예: 할머니국수 (종로))\n");
        prompt.append("날씨 조언: (예: 오늘처럼 맑은 날에는 시원한 콩국수로 더위를 식혀보세요!)\n");
        prompt.append("오늘 날씨 요약: (예: 서울 오늘은 맑음, 기온 25°C, 습도 60%, 바람 약함, 체감온도 27°C로 쾌적한 날씨입니다.)\n");
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.isBlank();
    }
    
    private boolean isValidWeatherDesc(String weatherDescKo) {
        return isNotEmpty(weatherDescKo) && !weatherDescKo.equals("알 수 없음");
    }
    
    private boolean hasDetailedWeatherInfo(String temp, Double feelsLike, String humidity, 
                                         Double windSpeed, Double rain1h) {
        return temp != null || feelsLike != null || humidity != null || windSpeed != null || rain1h != null;
    }
}
