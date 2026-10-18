/**
 * This program is the error thrown when the JWT caller is not allowed
 * to create a booking. BK-01 create is for customers.
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
        super("You are not allowed to create a booking");
    }
}
