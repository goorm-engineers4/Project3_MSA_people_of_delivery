package com.example.cloudfour.authservice.domain.passport.service;

import com.example.cloudfour.authservice.domain.passport.dto.PassportRequestDTO;
import com.example.cloudfour.authservice.domain.passport.dto.PassportResponseDTO;
import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PassportService 단위 테스트")
class PassportServiceTest {

    @Mock PassportUtil passportUtil;
    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> ops;

    @InjectMocks PassportService service;

    UUID userId;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(ops);
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("기존 PassportId 존재 시 캐시에서 불러와 반환")
    void create_uses_cache_when_exists() {
        String passportId = "pid-123";
        String userKey = "passport:user:" + userId;
        String cacheKey = "passport:" + passportId;
        when(ops.get(userKey)).thenReturn(passportId);
        when(ops.get(cacheKey)).thenReturn("pdata");

        Passport cached = Passport.builder()
                .passportId(passportId)
                .userId(userId)
                .role("USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .authLevel("STANDARD")
                .build();
        when(passportUtil.deserialize("pdata")).thenReturn(cached);

        var req = PassportRequestDTO.builder().userId(userId).role("USER").build();
        PassportResponseDTO out = service.createPassport(req);

        assertThat(out.getPassportId()).isEqualTo(passportId);
        assertThat(out.getUserId()).isEqualTo(userId);
        assertThat(out.getRole()).isEqualTo("USER");
        assertThat(out.getPassportData()).isEqualTo("pdata");
        verify(passportUtil, never()).serialize(any());
    }

    @Test
    @DisplayName("기존 Passport 없으면 생성 후 캐시에 저장")
    void create_new_and_cache() {
        when(ops.get("passport:user:" + userId)).thenReturn(null);
        when(passportUtil.serialize(any(Passport.class))).thenReturn("ser-data");

        var req = PassportRequestDTO.builder().userId(userId).role("OWNER").build();
        PassportResponseDTO out = service.createPassport(req);

        assertThat(out.getUserId()).isEqualTo(userId);
        assertThat(out.getRole()).isEqualTo("OWNER");
        assertThat(out.getPassportData()).isEqualTo("ser-data");

        verify(ops, atLeast(1)).set(startsWith("passport:"), eq("ser-data"), eq(5L), eq(TimeUnit.MINUTES));
        verify(ops, atLeast(1)).set(eq("passport:user:" + userId), anyString(), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("validatePassport: 유효한 데이터면 true")
    void validate_valid_true() {
        String pid = "pid-1";
        String cacheKey = "passport:" + pid;
        when(ops.get(cacheKey)).thenReturn("pdata");

        Passport p = Passport.builder()
                .passportId(pid)
                .userId(userId)
                .role("USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(30))
                .authLevel("HIGH")
                .build();
        when(passportUtil.deserialize("pdata")).thenReturn(p);

        assertThat(service.validatePassport(pid)).isTrue();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    @DisplayName("validatePassport: 만료/무효면 false, 캐시 삭제")
    void validate_invalid_false_and_delete() {
        String pid = "pid-2";
        String cacheKey = "passport:" + pid;
        when(ops.get(cacheKey)).thenReturn("pdata");

        Passport p = Passport.builder()
                .passportId(pid)
                .userId(userId)
                .role("USER")
                .issuedAt(Instant.now().minusSeconds(60))
                .expiresAt(Instant.now().minusSeconds(1))
                .authLevel("STANDARD")
                .build();
        when(passportUtil.deserialize("pdata")).thenReturn(p);

        assertThat(service.validatePassport(pid)).isFalse();
        verify(redisTemplate).delete(cacheKey);
    }

    @Test
    @DisplayName("validatePassport: 캐시에 없으면 false")
    void validate_not_found_false() {
        when(ops.get("passport:pid-x")).thenReturn(null);
        assertThat(service.validatePassport("pid-x")).isFalse();
    }
}
