package com.nicheflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.nicheflow.TestUsers;
import com.nicheflow.config.JwtProperties;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.security.AuthenticatedUser;

import io.jsonwebtoken.JwtException;

class JwtServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
    private static final String SECRET = base64("nicheflow-test-signing-key-32-bytes!");
    private static final Duration TTL = Duration.ofHours(1);

    private final User user = TestUsers.withId(42L, "ada@example.com", "$2a$10$hashed", Role.SUPPLIER);

    @Test
    void issuedTokenCarriesTheUserIdEmailAndRole() {
        JwtService service = serviceAt(NOW);

        AuthenticatedUser caller = service.parse(service.issue(user));

        assertThat(caller).isEqualTo(new AuthenticatedUser(42L, "ada@example.com", Role.SUPPLIER));
    }

    @Test
    void tokenIsRejectedOnceItHasExpired() {
        String token = serviceAt(NOW).issue(user);

        JwtService afterExpiry = serviceAt(NOW.plus(TTL).plusSeconds(1));

        assertThatThrownBy(() -> afterExpiry.parse(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenIsStillValidJustBeforeItExpires() {
        String token = serviceAt(NOW).issue(user);

        JwtService justBeforeExpiry = serviceAt(NOW.plus(TTL).minusSeconds(1));

        assertThat(justBeforeExpiry.parse(token).id()).isEqualTo(42L);
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        String foreignToken = new JwtService(
                new JwtProperties(base64("a-completely-different-key-32-byte!!"), TTL),
                Clock.fixed(NOW, ZoneOffset.UTC)).issue(user);

        JwtService service = serviceAt(NOW);

        assertThatThrownBy(() -> service.parse(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedPayloadIsRejected() {
        JwtService service = serviceAt(NOW);
        String token = service.issue(user);

        String[] parts = token.split("\\.");
        String forgedClaims = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"1\",\"email\":\"attacker@example.com\",\"role\":\"ADMIN\"}"
                        .getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + forgedClaims + "." + parts[2];

        assertThatThrownBy(() -> service.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void expiresInSecondsReflectsTheConfiguredLifetime() {
        assertThat(serviceAt(NOW).expiresInSeconds()).isEqualTo(3600L);
    }

    @Test
    void aPlainTextSecretIsAcceptedAsWellAsABase64One() {
        // The dev default in application.yml is plain text, so this guards startup
        // on a fresh clone that has not set NICHEFLOW_JWT_SECRET.
        JwtService service = new JwtService(
                new JwtProperties("nicheflow-dev-only-insecure-signing-key-change-me", TTL),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(service.parse(service.issue(user)).email()).isEqualTo("ada@example.com");
    }

    private static JwtService serviceAt(Instant instant) {
        return new JwtService(new JwtProperties(SECRET, TTL), Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static String base64(String raw) {
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
