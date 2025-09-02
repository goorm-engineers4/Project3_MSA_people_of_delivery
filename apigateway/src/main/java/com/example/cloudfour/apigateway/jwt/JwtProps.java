package com.example.cloudfour.apigateway.jwt;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProps {
    private String jwkSetUri;
    private String issuer;
    private String audience;

    public void setJwkSetUri(String jwkSetUri) { this.jwkSetUri = jwkSetUri; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public void setAudience(String audience) { this.audience = audience; }
}
