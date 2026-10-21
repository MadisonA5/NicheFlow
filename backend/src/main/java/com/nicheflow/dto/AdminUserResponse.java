/**
 * This program is the ADM-01 user row an admin may read. It carries
 * the id, email, and role name, and it never carries a password
 * hash.
 *
 * File:   AdminUserResponse.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import com.nicheflow.model.User;

public record AdminUserResponse(Long id, String email, String role) {

    /**
     * from - This function copies the fields an admin console may
     * show. Role is the enum name. The password hash stays on the
     * User row.
     *
     * Parameters:
     * user - The stored account to project
     *
     * Return - The public admin view of that account
     */
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getEmail(),
                user.getRole().name());
    }
}
