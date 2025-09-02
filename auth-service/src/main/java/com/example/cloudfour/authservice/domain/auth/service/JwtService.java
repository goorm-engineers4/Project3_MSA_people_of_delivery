package com.example.cloudfour.authservice.domain.auth.service;

import com.example.cloudfour.authservice.properties.JwtProps;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final JwtProps props;

    public String createAccess(UUID userId, String role) {
        return encode(userId, role, safe(props.getAccessExpSeconds()), "access");
    }

    public String createRefresh(UUID userId, String role) {
        return encode(userId, role, safe(props.getRefreshExpSeconds()), "refresh");
    }

    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    public boolean isValid(String token) {
        try { decoder.decode(token); return true; }
        catch (JwtException e) { return false; }
    }

    public String userId(String token) { return decode(token).getSubject(); }
    public String role(String token)    { return decode(token).getClaimAsString("role"); }
    public long accessTtlSeconds()      { return safe(props.getAccessExpSeconds()); }
    public long refreshTtlSeconds()     { return safe(props.getRefreshExpSeconds()); }

    private String encode(UUID userId, String role, long ttlSeconds, String typ) {
        Instant iat = Instant.now();
        Instant exp = iat.plusSeconds(ttlSeconds);

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .keyId(props.getJwks().getKeyId())
                .build();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.getIssuer())
                .audience(List.of(props.getAudience()))
                .issuedAt(iat)
                .expiresAt(exp)
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("role", role)
                .claim("typ", typ)
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private long safe(Long v) {
        return (v == null || v < 1) ? 300L : v;
    }
}