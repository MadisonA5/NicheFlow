/**
 * This program is the login JSON body: the email and password the
 * caller wants checked.
 *
 * File:   LoginRequest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(

        @NotBlank String email,

        @NotBlank String password) {
}
