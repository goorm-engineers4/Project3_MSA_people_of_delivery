package com.example.cloudfour.aiservice.controller;

import com.example.cloudfour.aiservice.dto.WeatherMenuRequestDTO;
import com.example.cloudfour.aiservice.dto.WeatherMenuResponseDTO;
import com.example.cloudfour.aiservice.service.command.WeatherMenuCommandServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/ai/weather-menu")
@RequiredArgsConstructor
public class WeatherMenuController {
    
    private final WeatherMenuCommandServiceImpl weatherMenuCommandService;
    
    @PostMapping("/recommend")
    public Mono<WeatherMenuResponseDTO> recommendMenuByWeather(@RequestBody WeatherMenuRequestDTO request) {
        log.info("날씨 기반 메뉴 추천 요청: {} - {}", request.getCity(), request.getWeather());
        return weatherMenuCommandService.recommendMenuByWeather(request);
    }
}