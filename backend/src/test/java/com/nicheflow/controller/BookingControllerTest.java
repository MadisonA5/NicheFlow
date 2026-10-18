/**
 * This program proves the BK-01 HTTP surface uses the same JWT
 * AuthenticatedUser as listing writes: a customer create returns the
 * pending booking, a supplier is 403, and an overlap is 409.
 *
 * File:   BookingControllerTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.exception.BookingConflictException;
import com.nicheflow.exception.GlobalExceptionHandler;
import com.nicheflow.exception.InvalidBookingException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.BookingService;
import com.nicheflow.service.JwtService;

@WebMvcTest(BookingController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class BookingControllerTest {

    private static final Instant START = Instant.parse(
            "2026-11-01T15:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-01T16:00:00Z");
    private static final String CREATE_BODY = """
            {
              "listingId": 3,
              "startTime": "2026-11-01T15:00:00Z",
              "endTime": "2026-11-01T16:00:00Z"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookingService bookingService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * createPassesTheJwtPrincipalIntoTheService - This function
     * stubs jwtService.parse and checks BookingService.create
     * receives that customer, then returns a pending booking.
     */
    @Test
    void createPassesTheJwtPrincipalIntoTheService() throws Exception {
        AuthenticatedUser caller = customer(9L, "good-token");
        given(bookingService.create(eq(caller), eq(3L), eq(START),
                eq(END))).willReturn(pendingBooking());

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.listingId").value(3))
                .andExpect(jsonPath("$.customerId").value(9))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.startTime")
                        .value("2026-11-01T15:00:00Z"))
                .andExpect(jsonPath("$.endTime")
                        .value("2026-11-01T16:00:00Z"));

        verify(bookingService).create(eq(caller), eq(3L), eq(START),
                eq(END));
    }

    /**
     * createWithoutATokenReturns401 - This function hits create with
     * no Authorization header.
     */
    @Test
    void createWithoutATokenReturns401() throws Exception {
        mockMvc.perform(post("/api/bookings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());

        verify(bookingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * createWithASupplierTokenReturns403 - This function sends a
     * SUPPLIER JWT and expects the ROLE_CUSTOMER gate.
     */
    @Test
    void createWithASupplierTokenReturns403() throws Exception {
        given(jwtService.parse("supplier-token"))
                .willReturn(new AuthenticatedUser(7L, "ada@example.com",
                        Role.SUPPLIER));

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer supplier-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isForbidden());

        verify(bookingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * createWhenTheWindowOverlapsReturns409 - This function stubs the
     * exclusion-constraint failure so the client gets a conflict.
     */
    @Test
    void createWhenTheWindowOverlapsReturns409() throws Exception {
        AuthenticatedUser caller = customer(9L, "good-token");
        given(bookingService.create(eq(caller), eq(3L), eq(START),
                eq(END))).willThrow(new BookingConflictException());

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Booking conflict"));
    }

    /**
     * createWhenTheListingIsMissingReturns404 - This function stubs a
     * missing listing id so the client gets 404.
     */
    @Test
    void createWhenTheListingIsMissingReturns404() throws Exception {
        AuthenticatedUser caller = customer(9L, "good-token");
        given(bookingService.create(eq(caller), eq(99L), eq(START),
                eq(END))).willThrow(new ListingNotFoundException(99L));

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "listingId": 99,
                          "startTime": "2026-11-01T15:00:00Z",
                          "endTime": "2026-11-01T16:00:00Z"
                        }
                        """))
                .andExpect(status().isNotFound());
    }

    /**
     * createWhenTheWindowIsInvalidReturns400 - This function stubs a
     * window the service rejected.
     */
    @Test
    void createWhenTheWindowIsInvalidReturns400() throws Exception {
        AuthenticatedUser caller = customer(9L, "good-token");
        given(bookingService.create(eq(caller), eq(3L), eq(START),
                eq(START))).willThrow(new InvalidBookingException(
                        "endTime must be after startTime"));

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "listingId": 3,
                          "startTime": "2026-11-01T15:00:00Z",
                          "endTime": "2026-11-01T15:00:00Z"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail")
                        .value("endTime must be after startTime"));
    }

    /**
     * createWithAMissingEndTimeReturns400 - This function omits
     * endTime so bean validation rejects the body.
     */
    @Test
    void createWithAMissingEndTimeReturns400() throws Exception {
        customer(9L, "good-token");

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"listingId":3,"startTime":"2026-11-01T15:00:00Z"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.endTime").exists());

        verify(bookingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * customer - This function stubs jwtService.parse so the filter
     * places a CUSTOMER AuthenticatedUser in the security context.
     *
     * Parameters:
     * id - The JWT subject / user id
     * token - The fake compact JWT on the Authorization header
     *
     * Return - The principal the filter will inject
     */
    private AuthenticatedUser customer(long id, String token) {
        AuthenticatedUser caller = new AuthenticatedUser(id,
                "bob@example.com", Role.CUSTOMER);
        given(jwtService.parse(token)).willReturn(caller);
        return caller;
    }

    /**
     * pendingBooking - This function builds the booking the service
     * stub returns after a successful create.
     *
     * Return - A pending booking with id 11
     */
    private static Booking pendingBooking() {
        User customer = TestUsers.withId(9L, "bob@example.com",
                "$2a$10$hashed", Role.CUSTOMER);
        User supplier = TestUsers.withId(7L, "ada@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), supplier);

        return TestBookings.withId(11L, listing, customer, START, END);
    }
}
