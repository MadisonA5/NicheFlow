/**
 * This program is the error thrown when a BK-02 status change is not
 * one of the allowed moves. The message names the current status and
 * the status that was refused.
 *
 * File:   InvalidBookingTransitionException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

import com.nicheflow.model.BookingStatus;

public class InvalidBookingTransitionException extends RuntimeException {

    /**
     * InvalidBookingTransitionException - This function builds the
     * 400 message for a status change the state machine refuses.
     *
     * Parameters:
     * current - The status stored on the booking
     * next - The status the caller asked for
     */
    public InvalidBookingTransitionException(BookingStatus current,
            BookingStatus next) {
        super("Cannot move a booking from " + current + " to " + next);
    }
}
