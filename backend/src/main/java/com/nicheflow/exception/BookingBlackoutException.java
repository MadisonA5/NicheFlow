/**
 * This program is the error thrown when a booking create falls
 * inside a window the supplier marked as a blackout on that listing.
 *
 * File:   BookingBlackoutException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class BookingBlackoutException extends RuntimeException {

    /**
     * BookingBlackoutException - This function builds the 409 message
     * returned when a booking window overlaps a stored blackout.
     */
    public BookingBlackoutException() {
        super("That time window is unavailable");
    }
}
