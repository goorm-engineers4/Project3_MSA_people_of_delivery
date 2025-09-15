package com.example.cloudfour.aiservice.client;

import com.example.cloudfour.aiservice.dto.GeminiRequestDTO;
import com.example.cloudfour.aiservice.dto.GeminiResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {
    
    private final WebClient webClient;
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent}")
    private String apiUrl;
    
    public Mono<GeminiResponseDTO> generateContent(String prompt) {
        GeminiRequestDTO request = GeminiRequestDTO.builder()
                .contents(List.of(
                        GeminiRequestDTO.Content.builder()
                                .parts(List.of(
                                        GeminiRequestDTO.Part.builder()
                                                .text(prompt)
                                                .build()
                                ))
                                .build()
                ))
                .build();

        String url = apiUrl + "?key=" + apiKey;
        
        log.info("Gemini API 호출중...");
        
        return webClient.post()
                .uri(url)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GeminiResponseDTO.class)
                .doOnSuccess(response -> log.info("Gemini API 응답 성공"))
                .doOnError(error -> log.error("Gemini API 호출 실패: {}", error.getMessage()));
    }
}