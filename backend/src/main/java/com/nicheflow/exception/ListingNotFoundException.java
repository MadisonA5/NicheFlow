/**
 * This program is the error thrown when create/edit/deactivate names
 * a listing id that is not in the table.
 *
 * File:   ListingNotFoundException.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.exception;

public class ListingNotFoundException extends RuntimeException {

    /**
     * ListingNotFoundException - This function builds the 404 message
     * that names the missing listing id.
     *
     * Parameters:
     * listingId - The id the caller asked for
     */
    public ListingNotFoundException(Long listingId) {
        super("No listing found with id " + listingId);
    }
}
