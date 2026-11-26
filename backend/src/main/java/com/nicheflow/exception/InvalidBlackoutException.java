/**
 * This program is the error thrown when a blackout create cannot be
 * stored because the window itself does not move forward.
 *
 * File:   InvalidBlackoutException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class InvalidBlackoutException extends RuntimeException {

    /**
     * InvalidBlackoutException - This function builds the 400
     * message for a blackout create the service will not attempt.
     *
     * Parameters:
     * message - The reason the window was rejected
     */
    public InvalidBlackoutException(String message) {
        super(message);
    }
}
