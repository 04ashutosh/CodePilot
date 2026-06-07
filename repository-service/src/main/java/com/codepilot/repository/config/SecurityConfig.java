package com.codepilot.repository.config;

import com.codepilot.repository.security.InternalApiKeyFilter;
import com.codepilot.repository.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Production security configuration for the Repository Service.
 *
 * Filter chain order:
 * 1. InternalApiKeyFilter — checks X-Internal-Key header for service-to-service calls.
 * 2. JwtAuthFilter — validates Bearer JWT tokens for user requests.
 * 3. Spring Security authorization — enforces .authenticated() on /api/repos/**.
 *
 * This mirrors project-service's SecurityConfig with the addition of
 * the internal API key filter for service-to-service communication.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Actuator health checks are always public.
                        .requestMatchers("/actuator/**").permitAll()
                        // All repository endpoints require authentication.
                        .requestMatchers("/api/repos/**").authenticated()
                        // Deny everything else by default.
                        .anyRequest().denyAll()
                )
                // Internal API key filter runs FIRST — if it authenticates,
                // the JWT filter simply sees an already-authenticated context and skips.
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                // JWT filter runs SECOND — for user-facing requests with Bearer tokens.
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}