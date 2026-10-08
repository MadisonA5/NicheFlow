/**
 * This program binds the nicheflow.jwt settings so the signing secret
 * and token lifetime can be changed without recompiling.
 *
 * File:   JwtProperties.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "nicheflow.jwt")
public record JwtProperties(

        @NotBlank String secret,

        @NotNull Duration expiration) {
}
