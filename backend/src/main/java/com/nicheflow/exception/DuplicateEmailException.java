/**
 * This program is the error thrown when registration tries to reuse an
 * email that is already stored, including the unique-index race.
 *
 * File:   DuplicateEmailException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class DuplicateEmailException extends RuntimeException {

    /**
     * DuplicateEmailException - This function builds the 409 message
     * that names the email that was already taken.
     *
     * Parameters:
     * email - The address that already has an account
     */
    public DuplicateEmailException(String email) {
        super("An account already exists for " + email);
    }
}
