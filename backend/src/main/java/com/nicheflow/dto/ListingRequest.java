/**
 * This program is the JSON body for creating or editing a listing.
 * Price is limited to two decimal places and cannot be negative.
 * Location and description are required text.
 *
 * File:   ListingRequest.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ListingRequest(

        @NotBlank @Size(max = 255) String title,

        @NotBlank @Size(max = 255) String category,

        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @NotBlank @Size(max = 255) String location,

        @NotBlank @Size(max = 2000) String description) {
}
