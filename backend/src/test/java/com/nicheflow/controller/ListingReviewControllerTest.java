/**
 * This program proves the ADM-02 listing review HTTP surface: list is
 * public and returns every review stored across a listing's
 * bookings.
 *
 * File:   ListingReviewControllerTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestReviews;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.exception.GlobalExceptionHandler;
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Review;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.service.JwtService;
import com.nicheflow.service.ReviewService;

@WebMvcTest(ListingReviewController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ListingReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * listWithoutATokenReturnsStoredReviews - This function proves
     * visitors can read a listing's reviews without signing in.
     */
    @Test
    void listWithoutATokenReturnsStoredReviews() throws Exception {
        given(reviewService.listForListing(3L))
                .willReturn(List.of(storedReview()));

        mockMvc.perform(get("/api/listings/3/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(21))
                .andExpect(jsonPath("$[0].rating").value(5));
    }

    /**
     * listWithAnUnknownListingIdReturnsEmptyList - This function
     * proves an id with no stored reviews is an empty list, not an
     * error.
     */
    @Test
    void listWithAnUnknownListingIdReturnsEmptyList() throws Exception {
        given(reviewService.listForListing(99L)).willReturn(List.of());

        mockMvc.perform(get("/api/listings/99/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    /**
     * storedReview - This function builds the review the mocked
     * service returns for the list assertion.
     *
     * Return - A review with id 21 on listing 3
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
}
