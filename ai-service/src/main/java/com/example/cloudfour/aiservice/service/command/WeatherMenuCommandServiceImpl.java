package com.example.cloudfour.aiservice.service.command;

import com.example.cloudfour.aiservice.builder.WeatherMenuPromptBuilder;
import com.example.cloudfour.aiservice.client.GeminiClient;
import com.example.cloudfour.aiservice.client.WeatherClient;
import com.example.cloudfour.aiservice.client.UserServiceClient;
import com.example.cloudfour.aiservice.constants.WeatherConstants;
import com.example.cloudfour.aiservice.converter.WeatherMenuConverter;
import com.example.cloudfour.aiservice.dto.WeatherMenuRequestDTO;
import com.example.cloudfour.aiservice.dto.WeatherMenuResponseDTO;
import com.example.cloudfour.aiservice.entity.AiLog;
import com.example.cloudfour.aiservice.parser.WeatherMenuResponseParser;
import com.example.cloudfour.aiservice.repository.AiLogRepository;
import com.example.cloudfour.aiservice.constants.CityTranslator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherMenuCommandServiceImpl {
    
    private final GeminiClient geminiClient;
    private final WeatherClient weatherClient;
    private final AiLogRepository aiLogRepository;
    private final UserServiceClient userServiceClient;
    private final WeatherMenuResponseParser responseParser;
    private final WeatherMenuPromptBuilder promptBuilder;

    public Mono<WeatherMenuResponseDTO> recommendMenuByWeather(WeatherMenuRequestDTO request) {
        String city = request.getCity();
        String weather = request.getWeather();
        

        if (request.getUserId() != null) {
            return userServiceClient.getUserAddresses(request.getUserId().toString())
                    .flatMap(addresses -> {
                        if (!addresses.isEmpty()) {
                            String userCity = addresses.get(0).getAddress();
                            log.info("사용자 주소 기반 도시 설정: {}", userCity);
                            return processWeatherMenuRecommendation(userCity, weather);
                        } else {
                            String defaultCity = city != null ? city : "서울";
                            return processWeatherMenuRecommendation(defaultCity, weather);
                        }
                    })
                    .onErrorResume(error -> {
                        log.warn("사용자 주소 정보 조회 실패, 기본 도시 사용: {}", error.getMessage());
                        String defaultCity = city != null ? city : "서울";
                        return processWeatherMenuRecommendation(defaultCity, weather);
                    });
        } else {
            String defaultCity = city != null ? city : "서울";
            return processWeatherMenuRecommendation(defaultCity, weather);
        }
    }
    
    private Mono<WeatherMenuResponseDTO> processWeatherMenuRecommendation(String city, String weather) {
        String englishCity = CityTranslator.translateToEnglish(city);
        log.info("도시명 변환: {} -> {}", city, englishCity);

        if (!CityTranslator.isSupportedCity(city) && city.equals(englishCity)) {
            log.warn("지원하지 않는 도시입니다. 날씨 정보가 부정확할 수 있습니다: {}", city);
        }
        
        return weatherClient.getCurrentWeather(englishCity)
                .flatMap(weatherData -> {
                    String weatherDescKo = WeatherConstants.getWeatherDescKo(weatherData.getCode());
                    return generateWeatherMenuRecommendation(
                        city,
                        weather,
                        weatherDescKo,
                        weatherData.getTemperature(),
                        weatherData.getFeelsLike(),
                        weatherData.getHumidity(),
                        weatherData.getWindSpeed(),
                        weatherData.getRain1h()
                    ).map(response -> WeatherMenuConverter.toWeatherMenuResponseDTO(
                        response.getRecommendedMenu(),
                        response.getRestaurantName(),
                        response.getRestaurantAddress(),
                        response.getReasoning(),
                        response.getAlternativeMenus(),
                        response.getAlternativeRestaurants(),
                        response.getWeatherAdvice(),
                        response.getTodayWeatherSummary(),
                        response.getSuccess(),
                        response.getErrorMessage(),
                        weatherDescKo
                    ));
                });
    }

    private Mono<WeatherMenuResponseDTO> generateWeatherMenuRecommendation(String city, String weather, String weatherDescKo, String temp, Double feelsLike, String humidity, Double windSpeed, Double rain1h) {
        String prompt = promptBuilder.buildWeatherMenuPrompt(city, weather, weatherDescKo, temp, feelsLike, humidity, windSpeed, rain1h);
        String requestType = "WEATHER_MENU";
        return geminiClient.generateContent(prompt)
                .map(responseParser::parseGeminiResponse)
                .doOnNext(response -> {
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                        String resultJson = objectMapper.writeValueAsString(response);
                        log.info("AI 로그 저장 시도 - prompt 길이: {}, result 길이: {}", 
                                prompt != null ? prompt.length() : 0, 
                                resultJson != null ? resultJson.length() : 0);
                        saveAiLog(prompt, resultJson, true, null, requestType);
                    } catch (Exception e) {
                        log.error("AI 응답 JSON 변환 실패: {}", e.getMessage());
                        saveAiLog(prompt, "AI 응답 JSON 변환 실패: " + e.getMessage(), false, e.getMessage(), requestType);
                    }
                })
                .onErrorResume(error -> {
                    log.error("날씨 기반 메뉴 추천 실패: {}", error.getMessage());
                    WeatherMenuResponseDTO errorResponse = WeatherMenuConverter.toErrorResponse(
                            "날씨 기반 메뉴 추천에 실패했습니다: " + error.getMessage());
                    String resultJson;
                    try {
                        ObjectMapper objectMapper = new ObjectMapper();
                        objectMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
                        resultJson = objectMapper.writeValueAsString(errorResponse);
                    } catch (Exception e) {
                        resultJson = "AI 응답 JSON 변환 실패: " + e.getMessage();
                    }
                    saveAiLog(prompt, resultJson, false, error.getMessage(), requestType);
                    return Mono.just(errorResponse);
                });
    }

    private void saveAiLog(String question, String result, Boolean success, String errorMessage, String requestType) {
            log.info("AI 로그 저장 시작 - question 길이: {}, result 길이: {}, success: {}, requestType: {}", 
                    question != null ? question.length() : 0,
                    result != null ? result.length() : 0,
                    success,
                    requestType);
            
            if (question != null && question.length() > 100) {
                log.info("Question (첫 100자): {}", question.substring(0, 100) + "...");
            }
            if (result != null && result.length() > 100) {
                log.info("Result (첫 100자): {}", result.substring(0, 100) + "...");
            }
            
            AiLog aiLog = AiLog.builder()
                    .question(question)
                    .result(result)
                    .success(success)
                    .errorMessage(errorMessage)
                    .requestType(requestType)
                    .build();
            
            aiLogRepository.save(aiLog);
            log.info("AI 로그 저장 완료: ID={}, question 길이={}, result 길이={}", 
                    aiLog.getId(), 
                    aiLog.getQuestion() != null ? aiLog.getQuestion().length() : 0,
                    aiLog.getResult() != null ? aiLog.getResult().length() : 0);
    }
}
