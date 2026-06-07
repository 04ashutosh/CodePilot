package com.codepilot.repository.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates internal service-to-service calls using a shared API key.
 *
 * How it works:
 * - Backend services (AI Orchestrator, Vector Memory Service) include
 *   the header "X-Internal-Key: <shared-secret>" in their requests.
 * - This filter checks the key BEFORE the JWT filter runs.
 * - If the key matches, the request is authenticated as "internal-service"
 *   with ROLE_INTERNAL authority, and the JWT filter is skipped.
 * - If the key doesn't match, the request falls through to the JWT filter.
 *
 * This avoids the need for internal services to obtain JWT tokens.
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Key";

    @Value("${app.internal.api-key:codepilot-internal-key-2026}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String providedKey = request.getHeader(INTERNAL_KEY_HEADER);

        // Only process if the header is present. If not, let the JWT filter handle it.
        if (StringUtils.hasText(providedKey)) {
            if (providedKey.equals(internalApiKey)) {
                // Valid internal key — authenticate as internal service.
                UserDetails serviceUser = new User(
                        "internal-service", "",
                        List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                );
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(serviceUser, null, serviceUser.getAuthorities());

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Internal service authenticated via API key");
            } else {
                // Invalid key — reject immediately.
                log.warn("Invalid internal API key received from {}", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid internal API key\"}");
                return; // Do NOT continue the filter chain.
            }
        }

        filterChain.doFilter(request, response);
    }
}