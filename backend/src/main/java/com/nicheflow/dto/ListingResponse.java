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
import java.util.Map;

import com.nicheflow.model.Listing;
import com.nicheflow.repository.ReviewRepository.ListingRatingSummary;

public record ListingResponse(Long id, String title, String category,
        BigDecimal price, String location, String description,
        Long supplierId, boolean active, Double averageRating,
        long reviewCount) {

    /**
     * from - This function copies the public listing fields, including
     * the owning supplier's id from the JWT-backed User row, with no
     * ADM-02 rating data attached.
     *
     * Parameters:
     * listing - The stored listing to project
     *
     * Return - The public listing payload
     */
    public static ListingResponse from(Listing listing) {
        return from(listing, null, 0L);
    }

    /**
     * from - This function copies the public listing fields, plus the
     * ADM-02 average rating and review count already summarized for
     * this listing.
     *
     * Parameters:
     * listing - The stored listing to project
     * averageRating - The mean of every rating left on this listing,
     *     or null when it has no reviews
     * reviewCount - How many reviews were averaged
     *
     * Return - The public listing payload
     */
    public static ListingResponse from(Listing listing,
            Double averageRating, long reviewCount) {
        return new ListingResponse(listing.getId(), listing.getTitle(),
                listing.getCategory(), listing.getPrice(),
                listing.getLocation(), listing.getDescription(),
                listing.getSupplier().getId(), listing.isActive(),
                averageRating, reviewCount);
    }

    /**
     * from - This function copies the public listing fields, looking
     * up this listing's rating summary in an already-batched map (as
     * ListingService.ratingSummaries returns) and falling back to no
     * rating when this listing has no entry.
     *
     * Parameters:
     * listing - The stored listing to project
     * ratingSummaries - The rating summary for each listing id that
     *     has at least one review
     *
     * Return - The public listing payload
     */
    public static ListingResponse from(Listing listing,
            Map<Long, ListingRatingSummary> ratingSummaries) {
        ListingRatingSummary summary = ratingSummaries.get(listing.getId());
        if (summary == null) {
            return from(listing);
        }

        return from(listing, summary.getAverageRating(),
                summary.getReviewCount());
    }
}
