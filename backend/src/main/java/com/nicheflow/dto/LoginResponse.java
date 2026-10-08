/**
 * This program is the login JSON body returned to the client: a Bearer
 * token, its lifetime, and the public user fields.
 *
 * File:   LoginResponse.java
 * Course: AUCSC 325

 */
package com.nicheflow.dto;

public record LoginResponse(String token, String tokenType,
        long expiresInSeconds, UserResponse user) {

    /**
     * bearer - This function builds a login response that always uses
     * the Bearer token type the React client will send back.
     *
     * Parameters:
     * token - The signed JWT to return
     * expiresInSeconds - How long the token stays valid
     * user - The public fields of the logged-in account
     *
     * Return - The complete login payload
     */
    public static LoginResponse bearer(String token, long expiresInSeconds,
            UserResponse user) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, user);
    }
}
