/**
 * This program wires AUTH-02 security: BCrypt hashing, a stateless JWT
 * filter, public health, auth, and listing search endpoints, the
 * ADM-01 admin read gate, LST-01 listing writes, BK-01 booking creates,
 * BK-03 booking confirms, and a 401 when no token is present so the
 * React client can tell "log in again" from "forbidden".
 * Supplier-role enforcement is proven by /api/listings.
 * Customer-role enforcement is proven by /api/bookings.
 *
 * File:   SecurityConfig.java
 * Course: AUCSC 325 
 *    
 * References:
 * https://docs.spring.io/spring-security/reference/servlet/authentication/index.html
 *     - Spring Security servlet authentication
 */
package com.nicheflow.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nicheflow.security.JwtAuthenticationFilter;
import com.nicheflow.service.JwtService;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * passwordEncoder - This function supplies the BCrypt hasher used
     * on every register and login.
     *
     * Return - The shared PasswordEncoder bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * securityFilterChain - This function builds the stateless JWT
     * filter chain: public health and auth routes, the ADM-01 admin
     * read gate, listing writes, booking creates and confirms,
     * everything else authenticated, and 401 for a missing token.
     *
     * Parameters:
     * http - The Spring Security builder
     * jwtService - The service the Bearer filter uses to parse tokens
     *
     * Return - The configured filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            JwtService jwtService) throws Exception {
        // To turn off cookie CSRF and browser login for a JWT API
        disableBrowserAuthDefaults(http);

        // To return 401 when no token is present instead of 403
        useUnauthorizedEntryPoint(http);

        // To leave health, register, and login public, then gate
        // the ADM-01 reads and LST-01 writes
        authorizeHttpRequests(http);

        // To check the Bearer token before the username/password filter
        http.addFilterBefore(new JwtAuthenticationFilter(jwtService),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * disableBrowserAuthDefaults - This function turns off CSRF,
     * sessions, HTTP Basic, and form login because this API is
     * authenticated by a header, not a cookie.
     *
     * Parameters:
     * http - The Spring Security builder to mutate
     */
    private void disableBrowserAuthDefaults(HttpSecurity http)
            throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable());
    }

    /**
     * useUnauthorizedEntryPoint - This function maps an unauthenticated
     * request to 401 so the client can tell it apart from a role denial.
     *
     * Parameters:
     * http - The Spring Security builder to mutate
     */
    private void useUnauthorizedEntryPoint(HttpSecurity http)
            throws Exception {
        http.exceptionHandling(handling -> handling.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
    }

    /**
     * authorizeHttpRequests - This function opens GET /health and the
     * two auth POSTs and public listing search, keeps the ADM-01
     * admin read gate, limits listing writes to suppliers and booking
     * creates and confirms to customers, and booking cancels to a logged-in customer,
     * supplier, or admin. Every other request requires a login.
     *
     * Parameters:
     * http - The Spring Security builder to mutate
     */
    private void authorizeHttpRequests(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register",
                        "/api/auth/login").permitAll()
                // Visitors may browse active listings; listing writes
                // remain supplier-only below.
                .requestMatchers(HttpMethod.GET, "/api/listings").permitAll()
                .requestMatchers(HttpMethod.GET,
                        "/api/bookings/availability/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/account")
                        .authenticated()
                // ADM-01 gate. GET /api/admin/** is the real admin
                // console, and it stays ADMIN-only.
                .requestMatchers(HttpMethod.GET, "/api/admin/**")
                        .hasRole("ADMIN")
                // LST-01 writes: role gate uses the same JWT ROLE_
                // the filter already set. Ownership still uses
                // AuthenticatedUser.id() in ListingService.
                .requestMatchers(HttpMethod.POST, "/api/listings")
                        .hasRole("SUPPLIER")
                .requestMatchers(HttpMethod.PUT, "/api/listings/*")
                        .hasRole("SUPPLIER")
                .requestMatchers(HttpMethod.POST,
                        "/api/listings/*/deactivate")
                        .hasRole("SUPPLIER")
                // BK-01 create. Overlap rejection is in BookingService.
                .requestMatchers(HttpMethod.POST, "/api/bookings")
                        .hasRole("CUSTOMER")
                // BK-03 confirm. The customer role passes the filter.
                // Ownership is checked again in BookingService.
                .requestMatchers(HttpMethod.PUT, "/api/bookings/*/confirm")
                        .hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.PUT, "/api/bookings/*/accept")
                        .hasRole("SUPPLIER")
                // BK-04 cancel. The service decides which of these
                // three parties may cancel this booking.
                .requestMatchers(HttpMethod.PUT, "/api/bookings/*/cancel")
                        .hasAnyRole("CUSTOMER", "SUPPLIER", "ADMIN")
                .anyRequest().authenticated());
    }
}
