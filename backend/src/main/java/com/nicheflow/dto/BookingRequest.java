/**
 * This program is the JSON body for BK-01 booking create. The
 * customer names a listing and a UTC window. Status is not accepted
 * from the client; a new booking is pending.
 *
 * File:   BookingRequest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import java.time.Instant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(

        @NotNull @Min(1) Long listingId,

        @NotNull Instant startTime,

        @NotNull Instant endTime) {
}
