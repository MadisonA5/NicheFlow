/**
 * This program proves the ADM-02 booking review HTTP surface: list is
 * public, create uses the same JWT AuthenticatedUser as /api/auth/me
 * and accepts either a customer or a supplier, and a service-reported
 * failure maps to the right status code.
 *
 * File:   BookingReviewControllerTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestReviews;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.exception.GlobalExceptionHandler;
import com.nicheflow.exception.InvalidReviewException;
import com.nicheflow.exception.ReviewAccessDeniedException;
import com.nicheflow.exception.ReviewConflictException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Review;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.JwtService;
import com.nicheflow.service.ReviewService;

@WebMvcTest(BookingReviewController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class BookingReviewControllerTest {

    private static final String CREATE_BODY = """
            {"rating":5,"comment":"Great stay"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * createPassesTheJwtPrincipalIntoTheService - This function
     * checks ReviewService.create receives the caller parsed off the
     * Bearer token.
     */
    @Test
    void createPassesTheJwtPrincipalIntoTheService() throws Exception {
        AuthenticatedUser caller = customer(9L, "good-token");
        Review stored = storedReview();
        given(reviewService.create(eq(caller), eq(11L), eq(5),
                eq("Great stay"))).willReturn(stored);

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.bookingId").value(11))
                .andExpect(jsonPath("$.reviewerId").value(9))
                .andExpect(jsonPath("$.revieweeId").value(7))
                .andExpect(jsonPath("$.rating").value(5));

        verify(reviewService).create(eq(caller), eq(11L), eq(5),
                eq("Great stay"));
    }

    /**
     * createWithASupplierTokenAlsoSucceeds - This function proves the
     * ROLE_SUPPLIER side of the create gate is also open.
     */
    @Test
    void createWithASupplierTokenAlsoSucceeds() throws Exception {
        AuthenticatedUser caller = supplier(7L, "supplier-token");
        Review stored = storedReview();
        given(reviewService.create(eq(caller), eq(11L), eq(5),
                eq("Great stay"))).willReturn(stored);

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer supplier-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isCreated());
    }

    /**
     * createWithoutATokenReturns401 - This function hits create with
     * no Authorization header.
     */
    @Test
    void createWithoutATokenReturns401() throws Exception {
        mockMvc.perform(post("/api/bookings/11/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());

        verify(reviewService, never()).create(any(), any(), anyInt(), any());
    }

    /**
     * createWithAnAdminTokenReturns403 - This function sends an ADMIN
     * JWT and expects the CUSTOMER-or-SUPPLIER gate.
     */
    @Test
    void createWithAnAdminTokenReturns403() throws Exception {
        given(jwtService.parse("admin-token"))
                .willReturn(new AuthenticatedUser(4L, "admin@example.com",
                        Role.ADMIN));

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer admin-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isForbidden());

        verify(reviewService, never()).create(any(), any(), anyInt(), any());
    }

    /**
     * createWithARatingAboveFiveReturns400 - This function proves
     * bean validation rejects an out-of-range rating.
     */
    @Test
    void createWithARatingAboveFiveReturns400() throws Exception {
        customer(9L, "good-token");

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"rating":6,"comment":"Too high"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());

        verify(reviewService, never()).create(any(), any(), anyInt(), any());
    }

    /**
     * createWithAMissingRatingReturns400 - This function omits
     * rating so bean validation rejects the body.
     */
    @Test
    void createWithAMissingRatingReturns400() throws Exception {
        customer(9L, "good-token");

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"comment":"No rating"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.rating").exists());

        verify(reviewService, never()).create(any(), any(), anyInt(), any());
    }

    /**
     * createWhenTheServiceReportsInvalidReturns400 - This function
     * stubs a booking that has not reached completed.
     */
    @Test
    void createWhenTheServiceReportsInvalidReturns400() throws Exception {
        AuthenticatedUser caller = customer(9L, "good-token");
        given(reviewService.create(eq(caller), eq(11L), eq(5),
                eq("Great stay"))).willThrow(new InvalidReviewException(
                        "Reviews are only allowed after a completed booking"));

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isBadRequest());
    }

    /**
     * createWhenTheServiceReportsAccessDeniedReturns403 - This
     * function stubs a caller who is not a party on the booking.
     */
    @Test
    void createWhenTheServiceReportsAccessDeniedReturns403()
            throws Exception {
        AuthenticatedUser caller = customer(8L, "other-token");
        given(reviewService.create(eq(caller), eq(11L), eq(5),
                eq("Great stay"))).willThrow(
                        new ReviewAccessDeniedException());

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer other-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    /**
     * createWhenTheServiceReportsConflictReturns409 - This function
     * stubs a reviewer who already reviewed this booking.
     */
    @Test
    void createWhenTheServiceReportsConflictReturns409() throws Exception {
        AuthenticatedUser caller = customer(9L, "good-token");
        given(reviewService.create(eq(caller), eq(11L), eq(5),
                eq("Great stay"))).willThrow(new ReviewConflictException());

        mockMvc.perform(post("/api/bookings/11/reviews")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isConflict());
    }

    /**
     * listWithoutATokenReturnsStoredReviews - This function proves
     * visitors can read a booking's reviews without signing in.
     */
    @Test
    void listWithoutATokenReturnsStoredReviews() throws Exception {
        given(reviewService.listForBooking(11L))
                .willReturn(List.of(storedReview()));

        mockMvc.perform(get("/api/bookings/11/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].bookingId").value(11));
    }

    /**
     * storedReview - This function builds the review the mocked
     * service returns for create and list assertions.
     *
     * Return - A review with id 21 on booking 11
     */
    private static Review storedReview() {
        User supplier = TestUsers.withId(7L, "ada@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        User customer = TestUsers.withId(9L, "bob@example.com",
                "$2a$10$hashed", Role.CUSTOMER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), supplier);
        Booking booking = TestBookings.withId(11L, listing, customer,
                Instant.parse("2026-11-01T15:00:00Z"),
                Instant.parse("2026-11-02T15:00:00Z"));
        return TestReviews.withId(21L, booking, customer, supplier, 5,
                "Great stay");
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
     * supplier - This function stubs jwtService.parse so the filter
     * places a SUPPLIER AuthenticatedUser in the security context.
     *
     * Parameters:
     * id - The JWT subject / user id
     * token - The fake compact JWT on the Authorization header
     *
     * Return - The principal the filter will inject
     */
    private AuthenticatedUser supplier(long id, String token) {
        AuthenticatedUser caller = new AuthenticatedUser(id,
                "ada@example.com", Role.SUPPLIER);
        given(jwtService.parse(token)).willReturn(caller);
        return caller;
    }
}
