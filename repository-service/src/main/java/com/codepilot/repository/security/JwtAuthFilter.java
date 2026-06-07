package com.codepilot.repository.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Spring Security filter that runs once per request to authenticate
 * users via JWT tokens or gateway-forwarded headers.
 *
 * Authentication flow:
 * 1. Extract Bearer token from Authorization header.
 * 2. Validate the JWT signature and expiration.
 * 3. Extract the username and set SecurityContext.
 * 4. If no JWT, fall back to X-User-Username header (set by gateway).
 *
 * Mirror of project-service's JwtAuthFilter for consistency.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenValidator tokenValidator;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = parseJwt(request);
            if (jwt != null && tokenValidator.validateToken(jwt)) {
                String username = tokenValidator.getUsernameFromToken(jwt);

                UserDetails userDetails = new User(username, "", Collections.emptyList());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // If there's no JWT but gateway forwarded X-User-Username (e.g. inside private network),
                // we fall back to trust the gateway header.
                String gatewayUsername = request.getHeader("X-User-Username");
                if (StringUtils.hasText(gatewayUsername)) {
                    UserDetails userDetails = new User(gatewayUsername, "", Collections.emptyList());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT from the "Authorization: Bearer <token>" header.
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}