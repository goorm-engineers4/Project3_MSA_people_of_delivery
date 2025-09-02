package com.example.cloudfour.aiservice.parser;

import com.example.cloudfour.aiservice.converter.ProductDescriptionConverter;
import com.example.cloudfour.aiservice.dto.GeminiResponseDTO;
import com.example.cloudfour.aiservice.dto.ProductDescriptionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductDescriptionResponseParser {

    public ProductDescriptionResponseDTO parseGeminiResponse(GeminiResponseDTO geminiResponse) {
        if (geminiResponse.getCandidates() == null || geminiResponse.getCandidates().isEmpty()) {
            return ProductDescriptionConverter.toErrorResponse("AI 응답이 비어있습니다.");
        }

        String responseText = geminiResponse.getCandidates().get(0).getContent().getParts().get(0).getText();
        
        if (responseText == null || responseText.trim().isEmpty()) {
            return ProductDescriptionConverter.toErrorResponse("AI 응답 텍스트가 비어있습니다.");
        }

        log.info("AI 원본 응답: {}", responseText);
        
        String description = "";
        String marketingCopy = "";
        String keyFeatures = "";
        String suggestedTags = "";
        
        String[] lines = responseText.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            log.info("파싱 중인 줄: '{}'", line);
            
            if (line.contains("메뉴 설명") || line.contains("메뉴설명")) {
                description = line.replaceAll("^.*?[:.]\\s*", "").trim();
                log.info("메뉴 설명 파싱: '{}'", description);
            } else if (line.contains("마케팅 문구") || line.contains("마케팅문구")) {
                marketingCopy = line.replaceAll("^.*?[:.]\\s*", "").trim();
                log.info("마케팅 문구 파싱: '{}'", marketingCopy);
            } else if (line.contains("핵심 특징") || line.contains("핵심특징")) {
                keyFeatures = line.replaceAll("^.*?[:.]\\s*", "").trim();
                log.info("핵심 특징 파싱: '{}'", keyFeatures);
            } else if (line.contains("추천 태그") || line.contains("추천태그")) {
                suggestedTags = line.replaceAll("^.*?[:.]\\s*", "").trim();
                log.info("추천 태그 파싱: '{}'", suggestedTags);
            }
        }
        
        description = parseByKeywordIfEmpty(description, responseText, "메뉴 설명", "마케팅 문구", 5);
        marketingCopy = parseByKeywordIfEmpty(marketingCopy, responseText, "마케팅 문구", "핵심 특징", 6);
        keyFeatures = parseByKeywordIfEmpty(keyFeatures, responseText, "핵심 특징", "추천 태그", 5);
        suggestedTags = parseByKeywordIfEmpty(suggestedTags, responseText, "추천 태그", null, 5);
        
        return ProductDescriptionConverter.toSuccessResponse(
                description, 
                marketingCopy, 
                keyFeatures, 
                suggestedTags
        );
    }
    
    private String parseByKeywordIfEmpty(String currentValue, String responseText, String startKeyword, String endKeyword, int keywordLength) {
        if (!currentValue.isEmpty()) {
            return currentValue;
        }
        
        if (!responseText.contains(startKeyword)) {
            return currentValue;
        }
        
        int start = responseText.indexOf(startKeyword);
        int end = endKeyword != null ? responseText.indexOf(endKeyword) : responseText.length();
        
        if (endKeyword != null && end == -1) {
            end = responseText.indexOf("**" + endKeyword);
        }
        if (end == -1) {
            end = responseText.length();
        }
        
        if (start != -1 && end != -1 && start < end) {
            String parsed = responseText.substring(start + keywordLength, end).trim();
            log.info("키워드 검색으로 {} 파싱: '{}'", startKeyword, parsed);
            return parsed;
        }
        
        return currentValue;
    }
}
