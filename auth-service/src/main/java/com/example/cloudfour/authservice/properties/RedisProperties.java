package com.example.cloudfour.authservice.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "redis")
@Getter
public class RedisProperties {
    @Value("${spring.data.redis.port}")
    private int port;
    @Value("${spring.data.redis.host}")
    private String host;
}
