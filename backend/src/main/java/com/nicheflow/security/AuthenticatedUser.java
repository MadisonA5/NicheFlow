/**
 * This program is the caller identity carried by a verified JWT. It is
 * the principal placed in the SecurityContext and holds no password
 * material.
 *
 * File:   AuthenticatedUser.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.security;

import com.nicheflow.model.Role;

public record AuthenticatedUser(Long id, String email, Role role) {
}
