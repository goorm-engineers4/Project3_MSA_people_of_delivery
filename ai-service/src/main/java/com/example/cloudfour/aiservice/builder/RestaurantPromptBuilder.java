package com.example.cloudfour.aiservice.builder;

import com.example.cloudfour.aiservice.dto.RestaurantDescriptionRequestDTO;
import org.springframework.stereotype.Component;

@Component
public class RestaurantPromptBuilder {

    public String buildPrompt(RestaurantDescriptionRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        appendIntroduction(prompt);

        if (request.getStore() != null) {
            appendStoreInfo(prompt, request.getStore());
        }

        appendResponseFormat(prompt);
        
        return prompt.toString();
    }
    
    private void appendIntroduction(StringBuilder prompt) {
        prompt.append("아래 가게 정보를 참고하여, 손님이 방문하고 싶어질 만한 매력적인 가게 소개글을 한두 문장으로 자연스럽게 작성해 주세요. ");
        prompt.append("가게의 특징, 장점, 인기 메뉴, 분위기, 추천 포인트 등을 간단하게 소개해 주세요. ");
        prompt.append("정보가 부족하면 상상력을 발휘해 예시로 채워도 좋습니다.\n\n");
    }
    
    private void appendStoreInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        appendRequiredInfo(prompt, store);
        appendOptionalContactInfo(prompt, store);
        appendOptionalBusinessInfo(prompt, store);
        appendOptionalContentInfo(prompt, store);
        appendOptionalPricingInfo(prompt, store);
        appendOptionalRatingInfo(prompt, store);
        appendOptionalImageInfo(prompt, store);
    }
    
    private void appendRequiredInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        prompt.append("가게명: ").append(store.getName()).append("\n");
        prompt.append("카테고리: ").append(store.getStoreCategory()).append("\n");
    }
    
    private void appendOptionalContactInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        if (isNotEmpty(store.getAddress())) {
            prompt.append("주소: ").append(store.getAddress()).append("\n");
        }
        if (isNotEmpty(store.getPhone())) {
            prompt.append("전화번호: ").append(store.getPhone()).append("\n");
        }
    }
    
    private void appendOptionalBusinessInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        if (isNotEmpty(store.getOperationHours())) {
            prompt.append("영업시간: ").append(store.getOperationHours()).append("\n");
        }
        if (isNotEmpty(store.getClosedDays())) {
            prompt.append("휴무일: ").append(store.getClosedDays()).append("\n");
        }
    }
    
    private void appendOptionalContentInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        if (isNotEmpty(store.getContent())) {
            prompt.append("가게 소개: ").append(store.getContent()).append("\n");
        }
    }
    
    private void appendOptionalPricingInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        if (store.getMinPrice() != null) {
            prompt.append("최소주문금액: ").append(store.getMinPrice()).append("원\n");
        }
        if (store.getDeliveryTip() != null) {
            prompt.append("배달팁: ").append(store.getDeliveryTip()).append("원\n");
        }
    }
    
    private void appendOptionalRatingInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        if (store.getRating() != null) {
            prompt.append("평점: ").append(store.getRating()).append("\n");
        }
        if (store.getLikeCount() != null) {
            prompt.append("좋아요 수: ").append(store.getLikeCount()).append("\n");
        }
        if (store.getReviewCount() != null) {
            prompt.append("리뷰 수: ").append(store.getReviewCount()).append("\n");
        }
    }
    
    private void appendOptionalImageInfo(StringBuilder prompt, RestaurantDescriptionRequestDTO.StoreDTO store) {
        if (isNotEmpty(store.getStorePicture())) {
            prompt.append("가게 사진: ").append(store.getStorePicture()).append("\n");
        }
    }
    
    private void appendResponseFormat(StringBuilder prompt) {
        prompt.append("\n아래 형식에 맞춰 반드시 모든 항목을 채워서 응답해 주세요.\n");
        prompt.append("가게 설명: (예: 홍대입구 명물, '행복한 분식'! 신선한 재료로 만든 다양한 분식 메뉴로 여러분의 입맛을 사로잡습니다.)\n");
        prompt.append("환영 메시지: (예: 안녕하세요! 행복한 분식에 오신 것을 환영합니다. 정성껏 준비한 맛있는 분식으로 여러분의 하루에 행복을 더해드릴게요!)\n");
        prompt.append("분위기 설명: (예: 깔끔하고 활기 넘치는 분위기 속에서 맛있는 분식을 즐기실 수 있습니다.)\n");
        prompt.append("추천 메뉴: 3~5개, 쉼표로 구분 (예: 떡볶이, 튀김, 김밥)\n");
        prompt.append("추천 태그: 3~5개, 쉼표로 구분 (예: 분식, 가성비, 혼밥, 포장, 배달)\n");
        prompt.append("정보가 부족하면 창의적으로 예시를 채워도 좋습니다.\n");
    }
    
    private boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }
}
