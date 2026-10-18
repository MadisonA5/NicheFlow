/**
 * This program is the error thrown when a booking insert loses the
 * BK-01 overlap constraint. The other request already holds that
 * window on the listing.
 *
 * File:   BookingConflictException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class BookingConflictException extends RuntimeException {

    /**
     * BookingConflictException - This function builds the 409 message
     * returned when PostgreSQL rejects an overlapping window.
     */
    public BookingConflictException() {
        super("That time window is already booked");
    }
}
