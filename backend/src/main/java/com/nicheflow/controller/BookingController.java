/**
 * This program is the BK-01 HTTP surface: a customer books a listing
 * for a UTC window. The caller is the AuthenticatedUser from the
 * JWT, the same way listing writes read the principal.
 *
 * File:   BookingController.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.BookingRequest;
import com.nicheflow.dto.BookingResponse;
import com.nicheflow.model.Booking;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.BookingService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    /**
     * BookingController - This function stores the booking service
     * Spring injects so the endpoint can stay thin.
     *
     * Parameters:
     * bookingService - The service that inserts a pending booking
     */
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * create - This function books a listing for the JWT caller.
     * The new row is pending. An overlapping window is a conflict.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * request - The listing id and the UTC window
     *
     * Return - The persisted pending booking
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @Valid @RequestBody BookingRequest request) {
        // To persist the booking owned by this JWT subject
        Booking booking = bookingService.create(caller,
                request.listingId(), request.startTime(),
                request.endTime());

        return BookingResponse.from(booking);
    }
}
