/**
 * This program is the error thrown when login fails for a missing
 * account or a wrong password. Both cases share one message so the
 * endpoint cannot be used to enumerate emails.
 *
 * File:   InvalidCredentialsException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

public class InvalidCredentialsException extends RuntimeException {

    /**
     * InvalidCredentialsException - This function builds the single
     * 401 message used for every failed login.
     */
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
