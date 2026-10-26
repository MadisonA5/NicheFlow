/**
 * This program is the public listing payload returned after create,
 * edit, and deactivate. It exposes the supplier id, not the whole
 * User, so a password hash never leaves the API.
 *
 * File:   ListingResponse.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.dto;

import java.math.BigDecimal;

import com.nicheflow.model.Listing;

public record ListingResponse(Long id, String title, String category,
        BigDecimal price, String location, String description,
        Long supplierId, boolean active) {

    /**
     * from - This function copies the public listing fields, including
     * the owning supplier's id from the JWT-backed User row.
     *
     * Parameters:
     * listing - The stored listing to project
     *
     * Return - The public listing payload
     */
    public static ListingResponse from(Listing listing) {
        return new ListingResponse(listing.getId(), listing.getTitle(),
                listing.getCategory(), listing.getPrice(),
                listing.getLocation(), listing.getDescription(),
                listing.getSupplier().getId(), listing.isActive());
    }
}
