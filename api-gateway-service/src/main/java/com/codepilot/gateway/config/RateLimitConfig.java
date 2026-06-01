package com.codepilot.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
    @Bean
    public KeyResolver userKeyResolver(){
        // Rate limit based on the header X-User-Username (injected by JwtAuthenticationFilter).
        // Fallback to IP address if X-User-Username is missing
        return exchange -> Mono.justOrEmpty(exchange.getRequest().getHeaders()
                .getFirst("X-User-Username")).defaultIfEmpty(exchange.getRequest()
                .getRemoteAddress()!=null?exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                :"anonymous");
    }
}
