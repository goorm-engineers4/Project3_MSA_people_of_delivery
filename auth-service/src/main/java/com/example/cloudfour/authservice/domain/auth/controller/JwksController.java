package com.example.cloudfour.authservice.domain.auth.controller;


import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {
    private final RSAKey rsaKey;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        JWKSet publicSet = new JWKSet(rsaKey.toPublicJWK());
        return publicSet.toJSONObject();
    }
}
