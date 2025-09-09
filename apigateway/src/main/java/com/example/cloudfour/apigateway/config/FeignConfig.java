package com.example.cloudfour.apigateway.config;

import feign.Logger;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;


@Configuration
public class FeignConfig {
    
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
    
    @Bean
    public HttpMessageConverters httpMessageConverters() {
        HttpMessageConverter<?> jacksonConverter = new MappingJackson2HttpMessageConverter();
        return new HttpMessageConverters(jacksonConverter);
    }
}