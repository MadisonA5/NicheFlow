/**
 * This program is the error thrown when a booking request cannot be
 * stored: the window does not move forward, or the listing is not
 * available.
 *
 * File:   InvalidBookingException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class InvalidBookingException extends RuntimeException {

    /**
     * InvalidBookingException - This function builds the 400 message
     * for a booking create the service will not attempt.
     *
     * Parameters:
     * message - The reason the window or listing was rejected
     */
    public InvalidBookingException(String message) {
        super(message);
    }
}
