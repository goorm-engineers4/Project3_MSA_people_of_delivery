package com.example.cloudfour.authservice.domain.passport.service;

import com.example.cloudfour.authservice.domain.passport.dto.PassportRequestDTO;
import com.example.cloudfour.authservice.domain.passport.dto.PassportResponseDTO;
import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PassportService {
    
    private final PassportUtil passportUtil;
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String PASSPORT_CACHE_PREFIX = "passport:";
    private static final long PASSPORT_TTL_MINUTES = 5;
    
    public PassportResponseDTO createPassport(PassportRequestDTO request) {
        try {
            String existingPassportId = getExistingPassportId(request.getUserId());
            if (existingPassportId != null) {
                log.debug("기존 Passport 재사용: passportId={}", existingPassportId);
                return getPassportFromCache(existingPassportId);
            }
        } catch (Exception e) {
            log.warn("Redis 캐시 확인 실패, 새 Passport 생성: {}", e.getMessage());
        }

        String passportId = UUID.randomUUID().toString();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(PASSPORT_TTL_MINUTES * 60);
        
        Passport passport = Passport.builder()
                .passportId(passportId)
                .userId(request.getUserId())
                .role(request.getRole())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .authLevel(determineAuthLevel(request))
                .build();

        String passportData = passportUtil.serialize(passport);

        try {
            String cacheKey = PASSPORT_CACHE_PREFIX + passportId;
            redisTemplate.opsForValue().set(cacheKey, passportData, PASSPORT_TTL_MINUTES, TimeUnit.MINUTES);

            String userKey = PASSPORT_CACHE_PREFIX + "user:" + request.getUserId();
            redisTemplate.opsForValue().set(userKey, passportId, PASSPORT_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.debug("Passport 캐시 저장 완료: passportId={}", passportId);
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패, Passport는 생성됨: {}", e.getMessage());
        }
        
        log.info("Passport 생성 및 캐시 저장: passportId={}, userId={}", passportId, request.getUserId());
        
        return PassportResponseDTO.builder()
                .passportId(passportId)
                .userId(passport.getUserId())
                .role(passport.getRole())
                .issuedAt(passport.getIssuedAt())
                .expiresAt(passport.getExpiresAt())
                .authLevel(passport.getAuthLevel())
                .passportData(passportData)
                .build();
    }
    
    public boolean validatePassport(String passportId) {
        String cacheKey = PASSPORT_CACHE_PREFIX + passportId;
        String passportData = redisTemplate.opsForValue().get(cacheKey);
        
        if (passportData == null) {
            log.debug("Passport 캐시에서 찾을 수 없음: passportId={}", passportId);
            return false;
        }
        
        try {
            Passport passport = passportUtil.deserialize(passportData);
            boolean isValid = passport != null && passport.isValid();
            
            if (!isValid) {
                redisTemplate.delete(cacheKey);
                log.debug("만료된 Passport 제거: passportId={}", passportId);
            }
            
            return isValid;
        } catch (Exception e) {
            log.error("Passport 검증 실패: passportId={}", passportId, e);
            return false;
        }
    }
    
    private String getExistingPassportId(UUID userId) {
        String userKey = PASSPORT_CACHE_PREFIX + "user:" + userId;
        return redisTemplate.opsForValue().get(userKey);
    }
    
    private PassportResponseDTO getPassportFromCache(String passportId) {
        String cacheKey = PASSPORT_CACHE_PREFIX + passportId;
        String passportData = redisTemplate.opsForValue().get(cacheKey);
        
        if (passportData == null) {
            return null;
        }
        
        try {
            Passport passport = passportUtil.deserialize(passportData);
            return PassportResponseDTO.builder()
                    .passportId(passportId)
                    .userId(passport.getUserId())
                    .role(passport.getRole())
                    .issuedAt(passport.getIssuedAt())
                    .expiresAt(passport.getExpiresAt())
                    .authLevel(passport.getAuthLevel())
                    .passportData(passportData)
                    .build();
        } catch (Exception e) {
            log.error("캐시된 Passport 파싱 실패: passportId={}", passportId, e);
            return null;
        }
    }
    
    private String determineAuthLevel(PassportRequestDTO request) {
        return "HIGH";
    }
}
