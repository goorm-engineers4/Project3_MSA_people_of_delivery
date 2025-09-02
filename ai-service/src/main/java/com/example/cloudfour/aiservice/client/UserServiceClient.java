package com.example.cloudfour.aiservice.client;

import com.example.cloudfour.aiservice.commondto.UserResponseDTO;
import com.example.cloudfour.aiservice.commondto.UserAddressResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {
    
    private final WebClient webClient;
    
    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;
    
    public Mono<UserResponseDTO> getUserById(String userId) {
        log.info("사용자 정보 조회 요청: {}", userId);
        
        return webClient.get()
                .uri(userServiceUrl + "/api/users/{userId}", userId)
                .retrieve()
                .bodyToMono(UserResponseDTO.class)
                .doOnSuccess(user -> log.info("사용자 정보 조회 성공: {}", userId))
                .doOnError(error -> log.error("사용자 정보 조회 실패: {} - {}", userId, error.getMessage()));
    }
    
    public Mono<List<UserAddressResponseDTO>> getUserAddresses(String userId) {
        log.info("사용자 주소 정보 조회 요청: {}", userId);
        
        return webClient.get()
                .uri(userServiceUrl + "/api/users/{userId}/addresses", userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UserAddressResponseDTO>>() {})
                .doOnSuccess(addresses -> log.info("사용자 주소 정보 조회 성공: {} - {}개", userId, addresses.size()))
                .doOnError(error -> log.error("사용자 주소 정보 조회 실패: {} - {}", userId, error.getMessage()));
    }
}