/**
 * This program wires AUTH-02 security: BCrypt hashing, a stateless JWT
 * filter, public health and auth endpoints, the temporary admin ping
 * pending ADM-01, LST-01 listing writes, and a 401 when no token is
 * present so the React client can tell "log in again" from
 * "forbidden". Supplier-role enforcement is proven by /api/listings.
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
     * ping gate, listing writes, everything else authenticated, and
     * 401 for a missing token.
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
        // the ADM-01 ping and LST-01 writes
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
     * two auth POSTs, keeps the ADM-01 admin ping gate, and requires
     * a login for every other request.
     *
     * Parameters:
     * http - The Spring Security builder to mutate
     */
    private void authorizeHttpRequests(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register",
                        "/api/auth/login").permitAll()
                // Temporary AUTH-02 scaffolding for ADM-01 only.
                // Remove when the real admin console lands (Week 6).
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
                .anyRequest().authenticated());
    }
}
