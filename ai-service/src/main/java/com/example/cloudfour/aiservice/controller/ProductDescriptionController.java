package com.example.cloudfour.aiservice.controller;

import com.example.cloudfour.aiservice.dto.ProductDescriptionRequestDTO;
import com.example.cloudfour.aiservice.dto.ProductDescriptionResponseDTO;
import com.example.cloudfour.aiservice.service.command.ProductDescriptionCommandServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/ai/product-description")
@RequiredArgsConstructor
public class ProductDescriptionController {
    
    private final ProductDescriptionCommandServiceImpl productDescriptionCommandService;
    
    @PostMapping("/generate")
    public Mono<ProductDescriptionResponseDTO> generateProductDescription(@RequestBody ProductDescriptionRequestDTO request) {
        log.info("상품 설명 생성 요청: {}", request.getName());
        return productDescriptionCommandService.generateProductDescription(request);
    }
}