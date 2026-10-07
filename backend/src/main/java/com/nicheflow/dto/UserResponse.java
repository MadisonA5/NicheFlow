/**
 * This program is the public user payload returned after register,
 * login, and /me. It never includes a password hash.
 *
 * File:   UserResponse.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import com.nicheflow.model.Role;
import com.nicheflow.model.User;

public record UserResponse(Long id, String email, Role role) {

    /**
     * from - This function copies the safe fields off a persisted user
     * so the API never echoes a password hash.
     *
     * Parameters:
     * user - The stored account to project
     *
     * Return - The public id, email, and role
     */
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(),
                user.getRole());
    }
}
