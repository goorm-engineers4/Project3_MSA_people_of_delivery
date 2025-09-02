package com.example.cloudfour.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final ReactiveJwtDecoder decoder;

    public AuthFilter(ReactiveJwtDecoder decoder) {
        super(Config.class);
        this.decoder = decoder;
    }

    public static class Config {
        private boolean requireToken = true;
        public boolean isRequireToken() { return requireToken; }
        public void setRequireToken(boolean requireToken) { this.requireToken = requireToken; }
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();
            String token = extractToken(request);

            if (!StringUtils.hasText(token)) {
                if (config.requireToken) return unauthorized(exchange, "missing_token");
                return chain.filter(exchange);
            }

            return decoder.decode(token)
                    .flatMap(jwt -> chain.filter(exchange))
                    .onErrorResume(err -> unauthorized(exchange, "invalid_token"));
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
