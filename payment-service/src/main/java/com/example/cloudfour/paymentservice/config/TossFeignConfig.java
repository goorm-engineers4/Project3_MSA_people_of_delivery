package com.example.cloudfour.paymentservice.config;


import com.example.cloudfour.modulecommon.passport.interceptor.FeignPassportInterceptor;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class TossFeignConfig {

    @Value("${toss.secret-key}")
    private String secretKey;

    @Bean
    public RequestInterceptor tossAuthInterceptor() {
        return template -> {
            String encodedKey = Base64.getEncoder()
                    .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
            template.header("Authorization", "Basic " + encodedKey);
            template.header("Content-Type", "application/json");
        };
    }

    @Bean
    public FeignPassportInterceptor TossfeignPassportInterceptor(PassportUtil passportUtil) {
        return new FeignPassportInterceptor(passportUtil);
    }
}
