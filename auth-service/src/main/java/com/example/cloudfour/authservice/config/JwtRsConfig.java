package com.example.cloudfour.authservice.config;

import com.example.cloudfour.authservice.properties.JwtProps;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProps.class)
@RequiredArgsConstructor
public class JwtRsConfig {

    @Bean
    public RSAKey rsaJwk(JwtProps props) throws Exception {
        return new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID(props.getJwks().getKeyId())
                .generate();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        var jwkSet = new JWKSet(rsaKey);
        return (selector, ctx) -> selector.select(jwkSet);
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey, JwtProps props) throws JOSEException {
        NimbusJwtDecoder dec = NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(props.getIssuer());
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            var aud = jwt.getAudience();
            if (aud != null && aud.contains(props.getAudience())) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("bad_audience", "aud mismatch", null)
            );
        };
        dec.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return dec;
    }
}