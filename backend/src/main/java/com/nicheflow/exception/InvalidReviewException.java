/**
 * This program is the error thrown when a review create cannot be
 * stored because the rating is out of range or the booking it is
 * about has not reached completed.
 *
 * File:   InvalidReviewException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class InvalidReviewException extends RuntimeException {

    /**
     * InvalidReviewException - This function builds the 400 message
     * for a review create the service will not attempt.
     *
     * Parameters:
     * message - The reason the review was rejected
     */
    public InvalidReviewException(String message) {
        super(message);
    }
}
