/**
 * This program is the error thrown when a client tries to register as
 * ADMIN, which AUTH-02 forbids as a privilege-escalation path.
 *
 * File:   RoleNotSelfAssignableException.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.exception;

import com.nicheflow.model.Role;

public class RoleNotSelfAssignableException extends RuntimeException {

    /**
     * RoleNotSelfAssignableException - This function builds the 400
     * message that names the role the client was not allowed to pick.
     *
     * Parameters:
     * role - The role that cannot be self-assigned
     */
    public RoleNotSelfAssignableException(Role role) {
        super("The " + role + " role cannot be assigned through registration");
    }
}
