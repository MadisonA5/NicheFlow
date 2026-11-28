package com.nicheflow.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.ReviewRepository.ListingRatingSummary;
import com.nicheflow.repository.ReviewRepository.UserRatingSummary;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.JwtService;
import com.nicheflow.service.ListingService;
import com.nicheflow.service.ReviewService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingRepository listingRepository;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private ListingService listingService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void customerReceivesOnlyTheirBookings() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "customer@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings").isEmpty())
                .andExpect(jsonPath("$.bookings").isArray())
                .andExpect(jsonPath("$.bookings").isEmpty());

        verify(bookingRepository).findAllForCustomer(9L);
    }

    @Test
    void supplierReceivesTheirOwnListingsAndBookings() throws Exception {
        given(jwtService.parse("supplier-token"))
                .willReturn(new AuthenticatedUser(7L, "supplier@example.com",
                        Role.SUPPLIER));

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer supplier-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings").isArray())
                .andExpect(jsonPath("$.listings").isEmpty())
                .andExpect(jsonPath("$.bookings").isArray())
                .andExpect(jsonPath("$.bookings").isEmpty());

        verify(listingRepository).findAllBySupplierId(7L);
        verify(bookingRepository).findAllForSupplier(7L);
    }

    @Test
    void accountDataRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * supplierSeesTheCustomersStandingEvenOnAPendingRequest - This
     * function proves a supplier can judge a still-PENDING request
     * by the requesting customer's ADM-02 standing with other
     * suppliers, not just their own history with that customer.
     */
    @Test
    void supplierSeesTheCustomersStandingEvenOnAPendingRequest()
            throws Exception {
        given(jwtService.parse("supplier-token"))
                .willReturn(new AuthenticatedUser(7L, "supplier@example.com",
                        Role.SUPPLIER));

        User supplier = TestUsers.withId(7L, "supplier@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        User customer = TestUsers.withId(9L, "bob@example.com",
                "$2a$10$hashed", Role.CUSTOMER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), supplier);
        Booking pending = TestBookings.withId(11L, listing, customer,
                Instant.parse("2026-11-01T15:00:00Z"),
                Instant.parse("2026-11-02T15:00:00Z"));
        given(bookingRepository.findAllForSupplier(7L))
                .willReturn(List.of(pending));

        UserRatingSummary summary = new UserRatingSummary() {
            @Override
            public Long getUserId() {
                return 9L;
            }

            @Override
            public Double getAverageRating() {
                return 4.5;
            }

            @Override
            public Long getReviewCount() {
                return 2L;
            }
        };
        given(reviewService.customerRatingSummaries(List.of(9L), 7L))
                .willReturn(Map.of(9L, summary));

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer supplier-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[0].status").value("PENDING"))
                .andExpect(jsonPath("$.bookings[0].customerAverageRating")
                        .value(4.5))
                .andExpect(jsonPath("$.bookings[0].customerReviewCount")
                        .value(2));
    }

    /**
     * supplierSeesNullStandingForAFirstTimeCustomer - This function
     * proves a customer with no reviews reports a null average, not
     * an error.
     */
    @Test
    void supplierSeesNullStandingForAFirstTimeCustomer() throws Exception {
        given(jwtService.parse("supplier-token"))
                .willReturn(new AuthenticatedUser(7L, "supplier@example.com",
                        Role.SUPPLIER));

        User supplier = TestUsers.withId(7L, "supplier@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        User customer = TestUsers.withId(9L, "bob@example.com",
                "$2a$10$hashed", Role.CUSTOMER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), supplier);
        Booking pending = TestBookings.withId(11L, listing, customer,
                Instant.parse("2026-11-01T15:00:00Z"),
                Instant.parse("2026-11-02T15:00:00Z"));
        given(bookingRepository.findAllForSupplier(7L))
                .willReturn(List.of(pending));
        given(reviewService.customerRatingSummaries(List.of(9L), 7L))
                .willReturn(Map.of());

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer supplier-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[0].customerAverageRating")
                        .doesNotExist())
                .andExpect(jsonPath("$.bookings[0].customerReviewCount")
                        .value(0));
    }

    /**
     * supplierSeesTheirOwnListingsRatingSummary - This function
     * proves a supplier's own listings report the same ADM-02
     * average rating the listing endpoints report, instead of
     * resetting it to null/0 on the account page.
     */
    @Test
    void supplierSeesTheirOwnListingsRatingSummary() throws Exception {
        given(jwtService.parse("supplier-token"))
                .willReturn(new AuthenticatedUser(7L, "supplier@example.com",
                        Role.SUPPLIER));

        User supplier = TestUsers.withId(7L, "supplier@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), supplier);
        given(listingRepository.findAllBySupplierId(7L))
                .willReturn(List.of(listing));

        ListingRatingSummary summary = new ListingRatingSummary() {
            @Override
            public Long getListingId() {
                return 3L;
            }

            @Override
            public Double getAverageRating() {
                return 4.5;
            }

            @Override
            public Long getReviewCount() {
                return 2L;
            }
        };
        given(listingService.ratingSummaries(List.of(3L)))
                .willReturn(Map.of(3L, summary));

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer supplier-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings[0].averageRating").value(4.5))
                .andExpect(jsonPath("$.listings[0].reviewCount").value(2));
    }

    /**
     * bookingsReportWhetherTheCallerAlreadyReviewedThem - This
     * function proves reviewedByCaller is looked up in bulk for
     * every booking, for both a supplier's and a customer's own
     * account view.
     */
    @Test
    void bookingsReportWhetherTheCallerAlreadyReviewedThem()
            throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));

        User supplier = TestUsers.withId(7L, "supplier@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        User customer = TestUsers.withId(9L, "bob@example.com",
                "$2a$10$hashed", Role.CUSTOMER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), supplier);
        Booking completed = TestBookings.withId(11L, listing, customer,
                Instant.parse("2026-11-01T15:00:00Z"),
                Instant.parse("2026-11-02T15:00:00Z"));
        given(bookingRepository.findAllForCustomer(9L))
                .willReturn(List.of(completed));
        given(reviewService.reviewedBookingIds(List.of(11L), 9L))
                .willReturn(Set.of(11L));

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings[0].reviewedByCaller")
                        .value(true));
    }
}
