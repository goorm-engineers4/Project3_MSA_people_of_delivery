package com.example.cloudfour.aiservice.builder;

import com.example.cloudfour.aiservice.dto.ProductDescriptionRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class ProductPromptBuilder {

    public String buildPrompt(ProductDescriptionRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        appendIntroduction(prompt);
        appendMenuInfo(prompt, request);
        appendResponseFormat(prompt);

        return prompt.toString();
    }
    
    private void appendIntroduction(StringBuilder prompt) {
        prompt.append("아래 메뉴 정보를 참고하여, 사장님이 메뉴 등록 시 사용할 수 있는 다양한 설명을 작성해 주세요.\n\n");
    }
    
    private void appendMenuInfo(StringBuilder prompt, ProductDescriptionRequestDTO request) {
        appendRequiredInfo(prompt, request);
        appendOptionalInfo(prompt, request);
        prompt.append("\n");
    }
    
    private void appendRequiredInfo(StringBuilder prompt, ProductDescriptionRequestDTO request) {
        prompt.append("메뉴명: ").append(request.getName()).append("\n");
    }
    
    private void appendOptionalInfo(StringBuilder prompt, ProductDescriptionRequestDTO request) {
        if (isNotEmpty(request.getCategory())) {
            prompt.append("카테고리: ").append(request.getCategory()).append("\n");
        }
        if (isNotEmpty(request.getIngredients())) {
            prompt.append("재료: ").append(request.getIngredients()).append("\n");
        }
        if (isNotEmpty(request.getPrice())) {
            prompt.append("가격: ").append(request.getPrice()).append("\n");
        }
    }
    
    private void appendResponseFormat(StringBuilder prompt) {
        prompt.append("\n아래 형식에 맞춰 반드시 모든 항목을 채워서 응답해 주세요.\n");
        
        appendFormatExamples(prompt);
        
        prompt.append("정보가 부족하면 창의적으로 예시를 채워도 좋습니다.");
    }
    
    private void appendFormatExamples(StringBuilder prompt) {
        prompt.append("메뉴 설명: (예: 육즙 가득한 불고기를 특제 소스로 맛을 내어 풍성하게 담았습니다.)\n");
        prompt.append("마케팅 문구: (예: 달콤 짭짤한 불고기버거의 풍미를 느껴보세요!)\n");
        prompt.append("핵심 특징: (예: 신선한 쇠고기와 특제 소스의 완벽한 조화)\n");
        prompt.append("추천 태그: 3~5개, 쉼표로 구분 (예: 불고기,한식,육류,특제소스,든든한)\n");
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
