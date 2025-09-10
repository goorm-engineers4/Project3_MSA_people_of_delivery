package com.example.cloudfour.paymentservice.config;

import com.example.cloudfour.modulecommon.passport.interceptor.FeignPassportInterceptor;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {
    @Bean
    public FeignPassportInterceptor feignPassportInterceptor(PassportUtil passportUtil){
        return new FeignPassportInterceptor(passportUtil);
    }
}