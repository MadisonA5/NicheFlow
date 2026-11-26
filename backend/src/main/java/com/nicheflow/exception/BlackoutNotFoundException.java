/**
 * This program is the error thrown when a delete names a blackout id
 * that is not in the table, or that does not belong to the named
 * listing.
 *
 * File:   BlackoutNotFoundException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class BlackoutNotFoundException extends RuntimeException {

    /**
     * BlackoutNotFoundException - This function builds the 404
     * message that names the missing blackout id.
     *
     * Parameters:
     * blackoutId - The id the caller asked for
     */
    public BlackoutNotFoundException(Long blackoutId) {
        super("No blackout found with id " + blackoutId);
    }
}
