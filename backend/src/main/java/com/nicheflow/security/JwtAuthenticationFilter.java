/**
 * This program turns a verified JWT into an authenticated principal
 * with a ROLE_ authority for AUTH-02. It is not a Spring bean because
 * Boot would also register every Filter bean with the servlet
 * container and run this a second time outside the security chain.
 *
 * File:   JwtAuthenticationFilter.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-security/reference/servlet/architecture.html
 *     - Servlet filter chain
 */
package com.nicheflow.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nicheflow.service.JwtService;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    /**
     * JwtAuthenticationFilter - This function stores the JWT service
     * used to verify Bearer tokens on each request.
     *
     * Parameters:
     * jwtService - The service that parses and verifies tokens
     */
    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * doFilterInternal - This function authenticates a Bearer token
     * when one is present, then always continues the filter chain.
     *
     * Parameters:
     * request - The incoming HTTP request
     * response - The outgoing HTTP response
     * chain - The rest of the servlet filter chain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // To authenticate only when a Bearer token is present
        if (isBearerToken(header)) {
            applyBearerToken(header.substring(BEARER_PREFIX.length()));
        }

        // To continue the chain whether or not a token was supplied
        chain.doFilter(request, response);
    }

    /**
     * isBearerToken - This function checks whether the Authorization
     * header uses the Bearer scheme.
     *
     * Parameters:
     * header - The raw Authorization header, or null
     *
     * Return - True when the header starts with "Bearer "
     */
    private boolean isBearerToken(String header) {
        return header != null && header.startsWith(BEARER_PREFIX);
    }

    /**
     * applyBearerToken - This function stores a verified caller in the
     * security context, or clears it when the token is invalid so the
     * entry point can return 401.
     *
     * Parameters:
     * token - The compact JWT after the Bearer prefix
     */
    private void applyBearerToken(String token) {
        try {
            // To copy a verified token into the security context
            AuthenticatedUser caller = jwtService.parse(token);
            SecurityContextHolder.getContext()
                    .setAuthentication(buildAuthentication(caller));
        } catch (JwtException | IllegalArgumentException invalidToken) {
            // To leave the context empty so the entry point returns 401
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * buildAuthentication - This function wraps the caller in a Spring
     * authentication with a ROLE_ authority for later hasRole checks.
     *
     * Parameters:
     * caller - The identity parsed from a valid token
     *
     * Return - An authenticated token with no credentials stored
     */
    private UsernamePasswordAuthenticationToken buildAuthentication(
            AuthenticatedUser caller) {
        var authority = new SimpleGrantedAuthority(
                "ROLE_" + caller.role().name());

        return UsernamePasswordAuthenticationToken.authenticated(caller,
                null, List.of(authority));
    }
}
