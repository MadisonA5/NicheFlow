/**
 * This program is the booking-scoped review HTTP surface for ADM-02.
 * Create is open to either party on the booking; list is public, the
 * same way booking availability is public.
 *
 * File:   BookingReviewController.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.ReviewRequest;
import com.nicheflow.dto.ReviewResponse;
import com.nicheflow.model.Review;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.ReviewService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/bookings/{bookingId}/reviews")
public class BookingReviewController {

    private final ReviewService reviewService;

    /**
     * BookingReviewController - This function stores the review
     * service Spring injects so each endpoint can stay thin.
     *
     * Parameters:
     * reviewService - The service that creates and lists reviews
     */
    public BookingReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * list - This function returns every review stored for a
     * booking. Visitors may browse without a login.
     *
     * Parameters:
     * bookingId - The booking whose reviews are requested
     *
     * Return - The reviews for that booking, possibly empty
     */
    @GetMapping
    public List<ReviewResponse> list(
            @PathVariable @Min(1) Long bookingId) {
        return reviewService.listForBooking(bookingId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * create - This function adds a review the JWT caller leaves for
     * the other party on a booking that has reached completed.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * bookingId - The completed booking being reviewed; must be 1 or
     *     greater
     * request - The rating and optional comment
     *
     * Return - The persisted review
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long bookingId,
            @Valid @RequestBody ReviewRequest request) {
        Review review = reviewService.create(caller, bookingId,
                request.rating(), request.comment());

        return ReviewResponse.from(review);
    }
}
