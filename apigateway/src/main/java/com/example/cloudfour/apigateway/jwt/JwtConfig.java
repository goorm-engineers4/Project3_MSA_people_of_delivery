package com.example.cloudfour.apigateway.jwt;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

@Configuration
@EnableConfigurationProperties(JwtProps.class)
public class JwtConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder(JwtProps props) {
        NimbusReactiveJwtDecoder dec = NimbusReactiveJwtDecoder
                .withJwkSetUri(props.getJwkSetUri())
                .build();

        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(props.getIssuer());
        OAuth2TokenValidator<Jwt> withAudience = jwt -> {
            var aud = jwt.getAudience();
            return (aud != null && aud.contains(props.getAudience()))
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_audience", "aud mismatch", null)
            );
        };
        dec.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience));
        return dec;
    }
}
