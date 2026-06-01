package com.codepilot.gateway.filter;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private final SecretKey signingKey;

    public JwtAuthenticationFilter(@Value("${app.jwt.secret}") String jwtSecret) {
        super(Config.class);
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    @Data
    public static class Config {
        // No custom config parameters needed for now
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization Header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                // Parse JWT Claims
                Claims claims = Jwts.parser()
                        .verifyWith(signingKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.getSubject();

                // Mutate the incoming request to inject username header.
                // Downstream services can read this header directly instead of parsing JWT again.
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Username", username)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (ExpiredJwtException e) {
                return onError(exchange, "JWT token is expired", HttpStatus.UNAUTHORIZED);
            } catch (JwtException | IllegalArgumentException e) {
                return onError(exchange, "Invalid JWT token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        log.error("Authentication error: {}", err);
        return response.setComplete();
    }
}