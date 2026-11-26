/**
 * This program is the JSON body for BO-01 blackout create. The
 * supplier names a UTC window and an optional reason. The listing is
 * named in the URL, not the body.
 *
 * File:   BlackoutRequest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BlackoutRequest(

        @NotNull Instant startTime,

        @NotNull Instant endTime,

        @Size(max = 255) String reason) {
}
