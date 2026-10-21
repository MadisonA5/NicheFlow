/**
 * This program is the ADM-01 admin console: read-only pages of
 * users and bookings. The ADMIN role gate lives in SecurityConfig.
 * This controller only loads the page.
 *
 * File:   AdminController.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.AdminBookingPageResponse;
import com.nicheflow.dto.AdminUserPageResponse;
import com.nicheflow.model.Booking;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.service.ListingService;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;

    /**
     * AdminController - This function stores the repositories Spring
     * injects so each read can stay a single page query.
     *
     * Parameters:
     * userRepository - The users table access point
     * bookingRepository - The bookings table access point
     */
    public AdminController(UserRepository userRepository,
            BookingRepository bookingRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * users - This function returns one page of accounts for an
     * admin. Page and size use the listing-search defaults and caps.
     * The password hash is not part of the response.
     *
     * Parameters:
     * page - Zero-based page number, or omitted for 0
     * size - Page size, or omitted for 20
     *
     * Return - The page of admin user payloads
     */
    @GetMapping("/users")
    @Transactional(readOnly = true)
    public AdminUserPageResponse users(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // To load one page with the same limits as listing search
        Page<User> result = userRepository.findAll(
                ListingService.toPageable(page, size));

        return AdminUserPageResponse.from(result);
    }

    /**
     * bookings - This function returns one page of bookings for an
     * admin. The read stays open until each booking's listing and
     * customer ids are copied onto BookingResponse.
     *
     * Parameters:
     * page - Zero-based page number, or omitted for 0
     * size - Page size, or omitted for 20
     *
     * Return - The page of booking payloads
     */
    @GetMapping("/bookings")
    @Transactional(readOnly = true)
    public AdminBookingPageResponse bookings(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // To load one page with the same limits as listing search
        Page<Booking> result = bookingRepository.findAll(
                ListingService.toPageable(page, size));

        return AdminBookingPageResponse.from(result);
    }
}
