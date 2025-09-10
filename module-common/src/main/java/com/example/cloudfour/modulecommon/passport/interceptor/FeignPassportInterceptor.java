package com.example.cloudfour.modulecommon.passport.interceptor;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class FeignPassportInterceptor implements RequestInterceptor {
    private final PassportUtil passportUtil;

    @Override
    public void apply(RequestTemplate template) {
        Collection<String> existingPassports = template.headers().get("X-Passport");
        if (existingPassports != null && !existingPassports.isEmpty()) {
            String existingPassport = existingPassports.iterator().next();
            log.debug("기존 Passport를 내부 통신에 전달: {}", existingPassport);
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
}
