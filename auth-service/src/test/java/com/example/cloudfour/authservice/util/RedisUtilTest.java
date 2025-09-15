package com.example.cloudfour.authservice.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("RedisUtil 단위 테스트")
class RedisUtilTest {

    @Mock RedisTemplate<String, String> redisTemplate;
    @Mock ValueOperations<String, String> ops;

    @InjectMocks RedisUtil util;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(ops);
    }

    @Test
    @DisplayName("save/get/delete 위임 동작")
    void basic_ops() {
        util.save("k", "v");
        verify(ops).set("k", "v");

        when(ops.get("k")).thenReturn("v");
        assertThat(util.get("k")).isEqualTo("v");

        util.delete("k");
        verify(redisTemplate).delete("k");
    }

    @Test
    @DisplayName("setIfAbsent: TRUE/FALSE 반환")
    void set_if_absent() {
        when(ops.setIfAbsent(eq("k"), eq("v"), any(Duration.class))).thenReturn(Boolean.TRUE);
        assertThat(util.setIfAbsent("k", "v", Duration.ofSeconds(10))).isTrue();

        when(ops.setIfAbsent(eq("k"), eq("v"), any(Duration.class))).thenReturn(Boolean.FALSE);
        assertThat(util.setIfAbsent("k", "v", Duration.ofSeconds(10))).isFalse();
    }

    @Test
    @DisplayName("setWithTtl/expire 위임 동작")
    void set_with_ttl_and_expire() {
        util.setWithTtl("k", "v", Duration.ofSeconds(5));
        verify(ops).set(eq("k"), eq("v"), eq(Duration.ofSeconds(5)));

        util.expire("k", Duration.ofMinutes(1));
        verify(redisTemplate).expire("k", Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("incrWithTtl: 최초 증가 시 TTL 설정, 이후에는 미설정")
    void incr_with_ttl() {
        when(ops.increment("tries")).thenReturn(1L);
        long v1 = util.incrWithTtl("tries", Duration.ofMinutes(10));
        assertThat(v1).isEqualTo(1L);
        verify(redisTemplate).expire("tries", Duration.ofMinutes(10));

        when(ops.increment("tries")).thenReturn(2L);
        long v2 = util.incrWithTtl("tries", Duration.ofMinutes(10));
        assertThat(v2).isEqualTo(2L);
        verify(redisTemplate).expire("tries", Duration.ofMinutes(10));

        when(ops.increment("tries2")).thenReturn(null);
        long v3 = util.incrWithTtl("tries2", Duration.ofMinutes(10));
        assertThat(v3).isEqualTo(0L);
    }
}
