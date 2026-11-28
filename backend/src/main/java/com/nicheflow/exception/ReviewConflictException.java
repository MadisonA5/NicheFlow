/**
 * This program is the error thrown when a reviewer tries to leave a
 * second review for the same booking.
 *
 * File:   ReviewConflictException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class ReviewConflictException extends RuntimeException {

    /**
     * ReviewConflictException - This function builds the 409 message
     * returned when a review create cannot be stored because this
     * reviewer already reviewed this booking.
     */
    public ReviewConflictException() {
        super("You already reviewed this booking");
    }
}
