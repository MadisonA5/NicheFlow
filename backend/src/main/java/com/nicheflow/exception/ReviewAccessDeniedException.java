/**
 * This program is the error thrown when the JWT caller is neither
 * party on the booking a review names, so they may not leave one.
 *
 * File:   ReviewAccessDeniedException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class ReviewAccessDeniedException extends RuntimeException {

    /**
     * ReviewAccessDeniedException - This function builds the 403
     * message used when the caller is not a party on this booking.
     */
    public ReviewAccessDeniedException() {
        super("You are not allowed to review this booking");
    }
}
