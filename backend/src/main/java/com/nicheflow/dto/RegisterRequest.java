/**
 * This program is the sign-up JSON body. Role may only be CUSTOMER or
 * SUPPLIER; ADMIN accounts are never self-service. Passwords longer
 * than 72 bytes are rejected because BCrypt would silently truncate
 * them.
 *
 * File:   RegisterRequest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import com.nicheflow.model.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank @Size(min = 8, max = 72) String password,

        @NotNull Role role) {
}
