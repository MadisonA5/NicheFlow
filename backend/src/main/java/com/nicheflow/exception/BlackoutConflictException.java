/**
 * This program is the error thrown when a new blackout overlaps a
 * window already stored for that listing, whether the window is
 * another blackout or an existing booking.
 *
 * File:   BlackoutConflictException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class BlackoutConflictException extends RuntimeException {

    /**
     * BlackoutConflictException - This function builds the 409
     * message returned when a blackout create cannot be stored
     * because it overlaps another blackout already on that listing.
     */
    public BlackoutConflictException() {
        super("That window overlaps an existing blackout");
    }

    /**
     * BlackoutConflictException - This function builds the 409
     * message returned when a blackout create cannot be stored,
     * using the caller-supplied reason so a conflict with an active
     * booking is not reported as if it were another blackout.
     *
     * Parameters:
     * message - The reason this blackout could not be stored
     */
    public BlackoutConflictException(String message) {
        super(message);
    }
}
