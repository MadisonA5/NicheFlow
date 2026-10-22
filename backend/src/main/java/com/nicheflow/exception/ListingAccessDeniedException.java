/**
 * This program is the error thrown when the JWT caller is not allowed
 * to create, edit, or deactivate a listing. That covers a non-supplier
 * and a supplier who does not own the row.
 *
 * File:   ListingAccessDeniedException.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.exception;

public class ListingAccessDeniedException extends RuntimeException {

    /**
     * ListingAccessDeniedException - This function builds the 403
     * message used for every listing privilege or ownership failure.
     */
    public ListingAccessDeniedException() {
        super("You are not allowed to change this listing");
    }
}
