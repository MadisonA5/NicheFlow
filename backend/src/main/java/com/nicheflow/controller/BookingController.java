/**
 * This program is the booking HTTP surface. BK-01 creates a pending
 * booking. BK-03 confirms that booking for its customer after the
 * mocked Stripe charge. BK-04 cancels it and returns the refund.
 * The caller is the AuthenticatedUser from the JWT.
 *
 * File:   BookingController.java
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.BookingRequest;
import com.nicheflow.dto.BookingResponse;
import com.nicheflow.dto.BookingAvailabilityResponse;
import com.nicheflow.model.Booking;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.BookingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

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
     * bookingService - The service that inserts and confirms bookings
     */
    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * getUnavailablePeriods - This function exposes reserved date
     * windows without exposing customer or booking details.
     *
     * Parameters:
     * listingId - The listing whose unavailable dates are requested
     *
     * Return - Pending, accepted, and confirmed date windows
     */
    @GetMapping("/availability/{listingId}")
    public List<BookingAvailabilityResponse> getUnavailablePeriods(
            @PathVariable @Min(1) Long listingId) {
        return bookingService.getUnavailablePeriods(listingId);
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

    /**
     * confirm - This function confirms the caller's own pending
     * booking. The service charges before the status changes.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * bookingId - The pending booking to confirm; must be 1 or greater
     *
     * Return - The booking now stored as confirmed
     */
    @PutMapping("/{bookingId}/confirm")
    public BookingResponse confirm(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long bookingId) {
        // To charge and confirm using this JWT subject
        Booking booking = bookingService.confirm(caller, bookingId);

        return BookingResponse.from(booking);
    }

    /**
     * accept - This function accepts a pending booking request for a
     * listing owned by the supplier caller.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * bookingId - The pending booking to accept; must be 1 or greater
     *
     * Return - The booking now stored as accepted
     */
    @PutMapping("/{bookingId}/accept")
    public BookingResponse accept(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long bookingId) {
        Booking booking = bookingService.accept(caller, bookingId);

        return BookingResponse.from(booking);
    }

    /**
     * cancel - This function cancels a booking the caller is allowed
     * to cancel and returns the stored refund.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * bookingId - The booking to cancel; must be 1 or greater
     *
     * Return - The booking now stored as cancelled
     */
    @PutMapping("/{bookingId}/cancel")
    public BookingResponse cancel(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long bookingId) {
        // To cancel and store the refund for this JWT subject
        Booking booking = bookingService.cancel(caller, bookingId);

        return BookingResponse.from(booking);
    }
}
