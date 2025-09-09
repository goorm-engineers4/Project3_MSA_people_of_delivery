package com.example.cloudfour.authservice.config;

import com.example.cloudfour.modulecommon.util.PassportUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PassportConfig {
    
    @Bean
    public PassportUtil passportUtil() {
        return new PassportUtil();
    }
}

