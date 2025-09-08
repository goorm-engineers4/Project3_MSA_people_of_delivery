package com.example.cloudfour.modulecommon.passport.filter;

import com.example.cloudfour.modulecommon.dto.Passport;
import com.example.cloudfour.modulecommon.passport.security.PassportAuthenticationToken;
import com.example.cloudfour.modulecommon.util.PassportUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class InternalPassportFilter extends OncePerRequestFilter {
    
    private final PassportUtil passportUtil;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        
        String passportHeader = request.getHeader("X-Passport");
        
        if (StringUtils.hasText(passportHeader)) {
            try {
                Passport passport = passportUtil.deserialize(passportHeader);
                
                if (passport != null && passport.isValid()) {
                    Collection<? extends GrantedAuthority> authorities = 
                            (passport.getRole() != null && !passport.getRole().isBlank())
                                    ? AuthorityUtils.createAuthorityList(passport.getRole())
                                    : AuthorityUtils.NO_AUTHORITIES;

                    var authentication = new PassportAuthenticationToken(passport, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    log.debug("Passport 인증 성공: userId={}, role={}, authLevel={}", 
                            passport.getUserId(), passport.getRole(), passport.getAuthLevel());
                } else {
                    log.warn("유효하지 않은 Passport: {}", passportHeader);
                }
            } catch (Exception e) {
                log.error("Passport 파싱 실패: {}", passportHeader, e);
            }
        } else {
            log.debug("X-Passport 헤더가 없습니다");
        }
        
        chain.doFilter(request, response);
    }
}

