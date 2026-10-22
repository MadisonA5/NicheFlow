/**
 * This program is the error thrown when a booking id is not in the
 * table.
 *
 * File:   BookingNotFoundException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class BookingNotFoundException extends RuntimeException {

    /**
     * BookingNotFoundException - This function builds the 404 message
     * that names the missing booking id.
     *
     * Parameters:
     * bookingId - The id the caller asked for
     */
    public BookingNotFoundException(Long bookingId) {
        super("No booking found with id " + bookingId);
    }
}
