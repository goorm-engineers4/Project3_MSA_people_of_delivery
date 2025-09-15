package com.example.cloudfour.authservice.domain.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "token", timeToLive = 10)
@AllArgsConstructor
@Getter
@ToString
public class Token {
    @Id
    private String email;
    private String refreshToken;

}
