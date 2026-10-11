/**
 * This program is the error thrown when LST-02 search params cannot
 * be applied, such as a minPrice greater than maxPrice.
 *
 * File:   InvalidSearchException.java
 * Course: AUCSC 325 
 */
package com.nicheflow.exception;

public class InvalidSearchException extends RuntimeException {

    /**
     * InvalidSearchException - This function builds the 400 message
     * that explains why the search range was rejected.
     *
     * Parameters:
     * detail - The reason the search cannot run
     */
    public InvalidSearchException(String detail) {
        super(detail);
    }
}
