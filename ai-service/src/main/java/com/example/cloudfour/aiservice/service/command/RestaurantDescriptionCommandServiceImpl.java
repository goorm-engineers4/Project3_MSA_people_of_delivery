package com.example.cloudfour.aiservice.service.command;

import com.example.cloudfour.aiservice.builder.RestaurantPromptBuilder;
import com.example.cloudfour.aiservice.client.GeminiClient;
import com.example.cloudfour.aiservice.converter.RestaurantDescriptionConverter;
import com.example.cloudfour.aiservice.dto.RestaurantDescriptionRequestDTO;
import com.example.cloudfour.aiservice.dto.RestaurantDescriptionResponseDTO;
import com.example.cloudfour.aiservice.entity.AiLog;
import com.example.cloudfour.aiservice.parser.RestaurantDescriptionResponseParser;
import com.example.cloudfour.aiservice.repository.AiLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestaurantDescriptionCommandServiceImpl {
    
    private final GeminiClient geminiClient;
    private final AiLogRepository aiLogRepository;
    private final RestaurantDescriptionResponseParser responseParser;
    private final RestaurantPromptBuilder promptBuilder;
    public Mono<RestaurantDescriptionResponseDTO> generateRestaurantDescription(RestaurantDescriptionRequestDTO request) {
        String prompt = promptBuilder.buildPrompt(request);
        String requestType = "RESTAURANT_DESCRIPTION";
        return geminiClient.generateContent(prompt)
                .map(responseParser::parseGeminiResponse)
                .doOnNext(response -> {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
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
                    log.error("가게 설명 생성 실패: {}", error.getMessage());
                    RestaurantDescriptionResponseDTO errorResponse = RestaurantDescriptionConverter.toErrorResponse(
                            "가게 설명 생성에 실패했습니다: " + error.getMessage());
                    String resultJson = null;
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
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
