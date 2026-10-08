/**
 * This program issues and verifies HS256 JWTs so login can stay
 * stateless. The secret may be base64 or plain text so a fresh clone
 * can start without an encoding step.
 *
 * File:   JwtService.java
 * Course: AUCSC 325
 *
 * References:
 * https://github.com/jwtk/jjwt - JJWT library
 */
package com.nicheflow.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import com.nicheflow.config.JwtProperties;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.security.AuthenticatedUser;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String EMAIL_CLAIM = "email";

    private final SecretKey key;
    private final Duration expiration;
    private final Clock clock;

    /**
     * JwtService - This function builds the production service with
     * the UTC clock so token expiry is timezone-safe.
     *
     * Parameters:
     * properties - The signing secret and configured lifetime
     */
    public JwtService(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /**
     * JwtService - This function builds the service with an injectable
     * clock so expiry tests can freeze time.
     *
     * Parameters:
     * properties - The signing secret and configured lifetime
     * clock - The clock used for issued-at and expiry
     */
    JwtService(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(decodeSecret(properties.secret()));
        this.expiration = properties.expiration();
        this.clock = clock;
    }

    /**
     * issue - This function signs a compact JWT that carries the user's
     * id, email, and role for later requests.
     *
     * Parameters:
     * user - The account to encode in the token
     *
     * Return - The compact signed JWT
     */
    public String issue(User user) {
        // To stamp issued-at and expiry from the injectable clock
        Instant now = clock.instant();

        // To pack id, email, and role into a signed compact JWT
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(ROLE_CLAIM, user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * parse - This function verifies a compact JWT and copies its
     * identity claims into the security principal. It throws
     * JwtException when the token is malformed, expired, or forged.
     *
     * Parameters:
     * token - The compact JWT from the Authorization header
     *
     * Return - The caller identity stored in the token
     */
    public AuthenticatedUser parse(String token) {
        // To verify the signature and reject expired or forged tokens
        Claims claims = readSignedClaims(token);

        return toAuthenticatedUser(claims);
    }

    /**
     * expiresInSeconds - This function reports the configured token
     * lifetime so the login response can tell the client when to
     * refresh.
     *
     * Return - The lifetime in seconds
     */
    public long expiresInSeconds() {
        return expiration.toSeconds();
    }

    /**
     * readSignedClaims - This function parses and verifies the compact
     * JWT against our signing key.
     *
     * Parameters:
     * token - The compact JWT to verify
     *
     * Return - The claims from a valid signature
     */
    private Claims readSignedClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * toAuthenticatedUser - This function copies the subject, email,
     * and role claims into the principal the filter will store.
     *
     * Parameters:
     * claims - The verified JWT payload
     *
     * Return - The caller identity with no password material
     */
    private AuthenticatedUser toAuthenticatedUser(Claims claims) {
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                claims.get(EMAIL_CLAIM, String.class),
                Role.valueOf(claims.get(ROLE_CLAIM, String.class)));
    }

    /**
     * decodeSecret - This function turns the configured secret into
     * key bytes. Base64 is tried first so production can supply
     * random binary data; raw text is used when that decode fails.
     *
     * Parameters:
     * secret - The configured JWT secret
     *
     * Return - The bytes used to build the HMAC key
     */
    private static byte[] decodeSecret(String secret) {
        try {
            // To treat a base64 secret as binary key material
            return Decoders.BASE64.decode(secret);
        } catch (DecodingException notBase64) {
            // To fall back to the raw UTF-8 bytes for local setup
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
