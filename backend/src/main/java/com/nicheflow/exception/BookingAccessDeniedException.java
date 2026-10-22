/**
 * This program is the error thrown when the JWT caller is not allowed
 * to create a booking or to move one. BK-01 create is for customers.
 * BK-03 limits confirm, cancel, and complete to the parties named in
 * the booking rules.
 *
 * File:   BookingAccessDeniedException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class BookingAccessDeniedException extends RuntimeException {

    /**
     * BookingAccessDeniedException - This function builds the 403
     * message used when a non-customer tries to create a booking.
     */
    public BookingAccessDeniedException() {
        this("You are not allowed to create a booking");
    }

    /**
     * BookingAccessDeniedException - This function builds the 403
     * message for a caller who may not make this booking change.
     *
     * Parameters:
     * message - The reason this caller was refused
     */
    public BookingAccessDeniedException(String message) {
        super(message);
    }
}
