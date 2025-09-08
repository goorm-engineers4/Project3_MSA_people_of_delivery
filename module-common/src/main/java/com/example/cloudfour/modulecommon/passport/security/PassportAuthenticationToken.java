package com.example.cloudfour.modulecommon.passport.security;

import com.example.cloudfour.modulecommon.dto.Passport;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class PassportAuthenticationToken extends AbstractAuthenticationToken {
    
    private final Passport passport;
    
    public PassportAuthenticationToken(Passport passport, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.passport = passport;
        setAuthenticated(true);
    }
    
    @Override
    public Object getCredentials() {
        return passport;
    }

    @Override
    public String getName() {
        return passport != null ? passport.getUserId().toString() : null;
    }

    @Override
    public Object getPrincipal() {
        return passport;
    }
    
    public Passport getPassport() {
        return passport;
    }
}
