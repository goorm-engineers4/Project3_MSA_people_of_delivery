package com.example.cloudfour.authservice.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProps {
    private String issuer;
    private String audience;
    private long accessExpSeconds;
    private long refreshExpSeconds;
    private Jwks jwks = new Jwks();

    @Getter @Setter
    public static class Jwks {
        private String keyId;
        private boolean devGenerate = true; // dev only
    }
}
