/**
 * This program is the AUTH-01 HTTP surface: register, login, and the
 * /me endpoint that returns the caller from a verified JWT.
 *
 * File:   AuthController.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.LoginRequest;
import com.nicheflow.dto.LoginResponse;
import com.nicheflow.dto.RegisterRequest;
import com.nicheflow.dto.UserResponse;
import com.nicheflow.model.User;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.AuthService;
import com.nicheflow.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    /**
     * AuthController - This function stores the auth and JWT services
     * Spring injects so each endpoint can stay thin.
     *
     * Parameters:
     * authService - The service that registers and authenticates users
     * jwtService - The service that issues and reads login tokens
     */
    public AuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    /**
     * register - This function creates an account from a validated
     * sign-up body and returns only the public user fields.
     *
     * Parameters:
     * request - The email, password, and requested role
     *
     * Return - The persisted user without a password hash
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        // To persist the new account after hashing and uniqueness checks
        User user = authService.register(request.email(), request.password(),
                request.role());

        return UserResponse.from(user);
    }

    /**
     * login - This function checks credentials and then issues a Bearer
     * token the client can send on later requests.
     *
     * Parameters:
     * request - The email and password to verify
     *
     * Return - The token, its lifetime, and the public user fields
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // To reject unknown emails and wrong passwords the same way
        User user = authService.authenticate(request.email(),
                request.password());

        return toLoginResponse(user);
    }

    /**
     * me - This function returns the caller already resolved from the
     * Authorization header so the client can restore a session.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     *
     * Return - The public id, email, and role
     */
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthenticatedUser caller) {
        return new UserResponse(caller.id(), caller.email(), caller.role());
    }

    /**
     * toLoginResponse - This function wraps a fresh JWT and the public
     * user fields into the login payload.
     *
     * Parameters:
     * user - The account that just authenticated
     *
     * Return - The Bearer login response
     */
    private LoginResponse toLoginResponse(User user) {
        return LoginResponse.bearer(jwtService.issue(user),
                jwtService.expiresInSeconds(), UserResponse.from(user));
    }
}
