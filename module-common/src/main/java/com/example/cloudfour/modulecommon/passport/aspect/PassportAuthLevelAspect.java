package com.example.cloudfour.modulecommon.passport.aspect;

import com.example.cloudfour.modulecommon.dto.Passport;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Aspect
@Component
public class PassportAuthLevelAspect {
    
    @Around("@annotation(com.example.cloudfour.modulecommon.passport.annotation.RequireHighAuthLevel)")
    public Object checkHighAuthLevel(ProceedingJoinPoint joinPoint) throws Throwable {
        Passport passport = getPassportFromParameters(joinPoint.getArgs());
        
        if (passport == null) {
            log.warn("Passport를 찾을 수 없습니다");
            throw new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "Passport를 찾을 수 없습니다");
        }
        
        if (!passport.isHighAuthLevel()) {
            log.warn("높은 인증 레벨이 필요합니다: current={}", passport.getAuthLevel());
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN, "높은 인증 레벨이 필요합니다");
        }
        
        log.debug("높은 인증 레벨 검증 성공: authLevel={}", passport.getAuthLevel());
        return joinPoint.proceed();
    }
    
    private Passport getPassportFromParameters(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Passport) {
                return (Passport) arg;
            }
        }
        return null;
    }
}
