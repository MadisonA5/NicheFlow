/**
 * This program is the JSON body for ADM-02 review create. The caller
 * names the rating and an optional comment. The booking is named in
 * the URL, not the body, and the reviewee is derived by the service
 * from which party the caller is.
 *
 * File:   ReviewRequest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewRequest(

        @NotNull @Min(1) @Max(5) Integer rating,

        @Size(max = 1000) String comment) {
}
