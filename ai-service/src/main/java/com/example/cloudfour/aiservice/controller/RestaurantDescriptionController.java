package com.example.cloudfour.aiservice.controller;

import com.example.cloudfour.aiservice.dto.RestaurantDescriptionRequestDTO;
import com.example.cloudfour.aiservice.dto.RestaurantDescriptionResponseDTO;
import com.example.cloudfour.aiservice.service.command.RestaurantDescriptionCommandServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/ai/restaurant-description")
@RequiredArgsConstructor
public class RestaurantDescriptionController {
    
    private final RestaurantDescriptionCommandServiceImpl restaurantDescriptionCommandService;
    
    @PostMapping("/generate")
    public Mono<RestaurantDescriptionResponseDTO> generateRestaurantDescription(@RequestBody RestaurantDescriptionRequestDTO request) {
        log.info("가게 설명 생성 요청: {}", request.getStore() != null ? request.getStore().getName() : "");
        return restaurantDescriptionCommandService.generateRestaurantDescription(request);
    }
}