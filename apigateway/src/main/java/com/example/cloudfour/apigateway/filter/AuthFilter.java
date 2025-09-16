package com.example.cloudfour.apigateway.filter;

import com.example.cloudfour.apigateway.client.PassportClient;
import com.example.cloudfour.apigateway.dto.PassportRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final ReactiveJwtDecoder decoder;
    private final PassportClient passportClient;

    @Autowired
    public AuthFilter(ReactiveJwtDecoder decoder, @Lazy PassportClient passportClient) {
        super(Config.class);
        this.decoder = decoder;
        this.passportClient = passportClient;
    }

    public static class Config {
        private boolean requireToken = true;
        public boolean isRequireToken() { return requireToken; }
        public void setRequireToken(boolean requireToken) { this.requireToken = requireToken; }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();
            // actuator 요청은 인증 스킵
            if (path.startsWith("/actuator")) {
                return chain.filter(exchange);
            }
            var request = exchange.getRequest();
            String token = extractToken(request);

            if (!StringUtils.hasText(token)) {
                if (config.requireToken) return unauthorized(exchange, "missing_token");
                return chain.filter(exchange);
            }

            return decoder.decode(token)
                    .flatMap(jwt -> {
                        log.info("JWT 검증 성공, Passport 생성 시작");

                        String userId = jwt.getSubject();
                        String role = jwt.getClaimAsString("role");

                        if (userId == null) {
                            return Mono.error(new IllegalArgumentException("JWT에 userId가 없습니다"));
                        }
                        if (role == null) {
                            return Mono.error(new IllegalArgumentException("JWT에 권한이 없습니다"));
                        }

                        PassportRequestDTO passportRequest = PassportRequestDTO.builder()
                                .userId(UUID.fromString(userId))
                                .role(role)
                                .build();

                        log.info("Passport 생성 요청: userId={}, role={}", userId, role);

                        return Mono.fromCallable(() -> passportClient.createPassport(passportRequest))
                                .flatMap(passportResponse -> {
                                    ServerHttpRequest modifiedRequest = request.mutate()
                                            .header("X-Passport", passportResponse.getPassportData())
                                            .build();

                                    log.info("Passport 생성 완료, 헤더에 추가: passportId={}", passportResponse.getPassportId());
                                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                                });
                    })
                    .onErrorResume(err -> {
                        log.error("JWT 검증 또는 Passport 생성 실패", err);
                        return unauthorized(exchange, "invalid_token");
                    });
        };
    }

    private String extractToken(ServerHttpRequest request) {
        String bearer = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        return request.getHeaders().getFirst("Access");
    }

    private Mono<Void> unauthorized(org.springframework.web.server.ServerWebExchange exchange, String msg) {
        var res = exchange.getResponse();
        res.setStatusCode(HttpStatus.UNAUTHORIZED);
        res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"message\":\"" + msg + "\"}").getBytes(StandardCharsets.UTF_8);
        return res.writeWith(Mono.just(res.bufferFactory().wrap(body)));
    }
}
