package com.example.cloudfour.cartservice.config;

import com.example.cloudfour.modulecommon.error.RestTemplateResponseErrorHandler;
import com.example.cloudfour.modulecommon.passport.interceptor.PassportInterceptor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder, PassportInterceptor passportInterceptor) {
        return builder.errorHandler(new RestTemplateResponseErrorHandler())
                .interceptors(passportInterceptor)
                .build();
    }
}

