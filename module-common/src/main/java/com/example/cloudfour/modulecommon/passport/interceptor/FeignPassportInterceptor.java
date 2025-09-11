package com.example.cloudfour.modulecommon.passport.interceptor;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class FeignPassportInterceptor implements RequestInterceptor {
    private final PassportUtil passportUtil;
    
    @Value("${spring.application.name:}")
    private String applicationName;

    @Override
    public void apply(RequestTemplate template) {
        Collection<String> existingPassports = template.headers().get("X-Passport");
        if (existingPassports != null && !existingPassports.isEmpty()) {
            String existingPassport = existingPassports.iterator().next();
            log.debug("기존 Passport를 내부 통신에 전달: {}", existingPassport);
            return;
        }

        String url = template.url();
        log.debug("FeignPassportInterceptor - 요청 URL: {}", url);
        if (isPassportNotRequired(url)) {
            log.debug("Passport가 필요하지 않은 API입니다. URL: {}", url);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Passport) {
            Passport currentPassport = (Passport) authentication.getPrincipal();

            String passportData = passportUtil.serialize(currentPassport);
            template.header("X-Passport", passportData);

            log.debug("현재 사용자 Passport를 내부 통신에 전달: userId={}, role={}",
                    currentPassport.getUserId(), currentPassport.getRole());
            return;
        }

        log.error("내부 통신 시 Passport가 필요합니다");
        throw new RuntimeException("내부 통신 시 Passport가 필요합니다");
    }

    private boolean isPassportNotRequired(String url) {
        boolean isBasicException = url.contains("/api/passports/") ||
                                  url.contains("/.well-known/");

        if ("auth-service".equals(applicationName)) {
            return isBasicException ||
                   url.contains("/by-email") ||
                   url.contains("/verify-password") ||
                   url.contains("/exists") ||
                   url.contains("/email-verified") ||
                   url.contains("/change-password") ||
                   url.contains("/email-change/");
        }
        
        return isBasicException;
    }
}
