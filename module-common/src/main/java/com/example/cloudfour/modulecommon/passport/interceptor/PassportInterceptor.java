package com.example.cloudfour.modulecommon.passport.interceptor;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PassportInterceptor implements ClientHttpRequestInterceptor {
    
    private final PassportUtil passportUtil;
    
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) 
            throws IOException {

        String existingPassport = request.getHeaders().getFirst("X-Passport");
        if (existingPassport != null) {
            log.debug("기존 Passport를 내부 통신에 전달: {}", existingPassport);
            return execution.execute(request, body);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Passport) {
            Passport currentPassport = (Passport) authentication.getPrincipal();

            String passportData = passportUtil.serialize(currentPassport);
            request.getHeaders().add("X-Passport", passportData);
            
            log.debug("현재 사용자 Passport를 내부 통신에 전달: userId={}, role={}", 
                    currentPassport.getUserId(), currentPassport.getRole());
            return execution.execute(request, body);
        }

        log.error("내부 통신 시 Passport가 필요합니다");
        throw new RuntimeException("내부 통신 시 Passport가 필요합니다");
    }
}
