/**
 * This program is the public blackout payload returned after BO-01
 * create and list. It exposes the listing id, not the Listing row.
 *
 * File:   BlackoutResponse.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import java.time.Instant;

import com.nicheflow.model.ListingBlackout;

public record BlackoutResponse(Long id, Long listingId, Instant startTime,
        Instant endTime, String reason, Instant createdAt) {

    /**
     * from - This function copies the public blackout fields.
     *
     * Parameters:
     * blackout - The stored blackout to project
     *
     * Return - The public blackout payload
     */
    public static BlackoutResponse from(ListingBlackout blackout) {
        return new BlackoutResponse(blackout.getId(),
                blackout.getListing().getId(),
                blackout.getStartTime(),
                blackout.getEndTime(),
                blackout.getReason(),
                blackout.getCreatedAt());
    }
}
