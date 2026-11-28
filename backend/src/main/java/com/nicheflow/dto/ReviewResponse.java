/**
 * This program is the public review payload returned after ADM-02
 * create and list. It exposes the booking, reviewer, and reviewee
 * ids, not the entity rows.
 *
 * File:   ReviewResponse.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import java.time.Instant;

import com.nicheflow.model.Review;

public record ReviewResponse(Long id, Long bookingId, Long reviewerId,
        Long revieweeId, int rating, String comment, Instant createdAt) {

    /**
     * from - This function copies the public review fields.
     *
     * Parameters:
     * review - The stored review to project
     *
     * Return - The public review payload
     */
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(),
                review.getBooking().getId(),
                review.getReviewer().getId(),
                review.getReviewee().getId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}
