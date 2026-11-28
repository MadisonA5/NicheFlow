/**
 * This program is the listing-scoped review HTTP surface for ADM-02.
 * It is read-only: a visitor browsing a listing can see every review
 * its supplier has received across all of that listing's bookings.
 *
 * File:   ListingReviewController.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.ReviewResponse;
import com.nicheflow.service.ReviewService;

import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/listings/{listingId}/reviews")
public class ListingReviewController {

    private final ReviewService reviewService;

    /**
     * ListingReviewController - This function stores the review
     * service Spring injects so list can stay thin.
     *
     * Parameters:
     * reviewService - The service that lists reviews
     */
    public ListingReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * list - This function returns every review stored across every
     * booking on a listing. Visitors may browse without a login.
     *
     * Parameters:
     * listingId - The listing whose reviews are requested
     *
     * Return - The reviews for that listing, possibly empty
     */
    @GetMapping
    public List<ReviewResponse> list(
            @PathVariable @Min(1) Long listingId) {
        return reviewService.listForListing(listingId).stream()
                .map(ReviewResponse::from)
                .toList();
    }
}
