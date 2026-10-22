/**
 * This program proves the ADM-01 HTTP surface. An ADMIN token gets
 * one page of users and one page of bookings. A customer, a
 * supplier, and a missing token are turned away. The users body
 * never contains a password hash.
 *
 * File:   AdminControllerTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.JwtService;
import com.nicheflow.service.ListingService;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    private static final String PASSWORD_HASH = "$2a$10$not-for-the-wire";
    private static final Instant START = Instant.parse(
            "2026-11-01T15:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-01T16:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private JwtService jwtService;

    /**
     * adminListsUsers - This function sends an ADMIN token to
     * GET /api/admin/users and expects the page fields plus id,
     * email, and role. The password hash must be absent.
     */
    @Test
    void adminListsUsers() throws Exception {
        stubRole("admin-token", Role.ADMIN);
        Pageable firstPage = ListingService.toPageable(null, null);
        User stored = TestUsers.withId(9L, "bob@example.com",
                PASSWORD_HASH, Role.CUSTOMER);
        given(userRepository.findAll(firstPage)).willReturn(
                new PageImpl<>(List.of(stored), firstPage, 1));

        getWithToken("/api/admin/users", "admin-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(9))
                .andExpect(jsonPath("$.items[0].email")
                        .value("bob@example.com"))
                .andExpect(jsonPath("$.items[0].role").value("CUSTOMER"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(content().string(
                        not(containsString("passwordHash"))))
                .andExpect(content().string(
                        not(containsString(PASSWORD_HASH))));
    }

    /**
     * adminListsBookings - This function sends an ADMIN token to
     * GET /api/admin/bookings and expects the same page shape around
     * a BookingResponse.
     */
    @Test
    void adminListsBookings() throws Exception {
        stubRole("admin-token", Role.ADMIN);
        Pageable firstPage = ListingService.toPageable(null, null);
        given(bookingRepository.findAll(firstPage)).willReturn(
                new PageImpl<>(List.of(storedBooking()), firstPage, 1));

        getWithToken("/api/admin/bookings", "admin-token")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(11))
                .andExpect(jsonPath("$.items[0].listingId").value(3))
                .andExpect(jsonPath("$.items[0].customerId").value(9))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    /**
     * customerIsForbiddenOnUsers - This function sends a CUSTOMER
     * token to the user list and expects 403.
     */
    @Test
    void customerIsForbiddenOnUsers() throws Exception {
        stubRole("customer-token", Role.CUSTOMER);

        getWithToken("/api/admin/users", "customer-token")
                .andExpect(status().isForbidden());

        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    /**
     * customerIsForbiddenOnBookings - This function sends a CUSTOMER
     * token to the booking list and expects 403.
     */
    @Test
    void customerIsForbiddenOnBookings() throws Exception {
        stubRole("customer-token", Role.CUSTOMER);

        getWithToken("/api/admin/bookings", "customer-token")
                .andExpect(status().isForbidden());

        verify(bookingRepository, never()).findAll(any(Pageable.class));
    }

    /**
     * supplierIsForbiddenOnUsers - This function sends a SUPPLIER
     * token to the user list and expects 403.
     */
    @Test
    void supplierIsForbiddenOnUsers() throws Exception {
        stubRole("supplier-token", Role.SUPPLIER);

        getWithToken("/api/admin/users", "supplier-token")
                .andExpect(status().isForbidden());

        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    /**
     * supplierIsForbiddenOnBookings - This function sends a SUPPLIER
     * token to the booking list and expects 403.
     */
    @Test
    void supplierIsForbiddenOnBookings() throws Exception {
        stubRole("supplier-token", Role.SUPPLIER);

        getWithToken("/api/admin/bookings", "supplier-token")
                .andExpect(status().isForbidden());

        verify(bookingRepository, never()).findAll(any(Pageable.class));
    }

    /**
     * usersWithoutATokenReturns401 - This function hits the user
     * list with no Authorization header.
     */
    @Test
    void usersWithoutATokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    /**
     * bookingsWithoutATokenReturns401 - This function hits the
     * booking list with no Authorization header.
     */
    @Test
    void bookingsWithoutATokenReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/bookings"))
                .andExpect(status().isUnauthorized());

        verify(bookingRepository, never()).findAll(any(Pageable.class));
    }

    /**
     * stubRole - This function makes jwtService.parse return a
     * principal with the given role.
     *
     * Parameters:
     * token - The fake compact JWT the filter will parse
     * role - The role that token should carry
     */
    private void stubRole(String token, Role role) {
        given(jwtService.parse(token)).willReturn(
                new AuthenticatedUser(1L, "ada@example.com", role));
    }

    /**
     * getWithToken - This function GETs a path with a Bearer token.
     *
     * Parameters:
     * path - The admin endpoint to call
     * token - The fake compact JWT already stubbed for the filter
     *
     * Return - The MockMvc result so the test can assert status
     */
    private ResultActions getWithToken(String path, String token)
            throws Exception {
        return mockMvc.perform(get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    /**
     * storedBooking - This function builds the booking the repository
     * stub returns on the first page.
     *
     * Return - A pending booking with id 11
     */
    private static Booking storedBooking() {
        User customer = TestUsers.withId(9L, "bob@example.com",
                PASSWORD_HASH, Role.CUSTOMER);
        User supplier = TestUsers.withId(7L, "ada@example.com",
                PASSWORD_HASH, Role.SUPPLIER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), supplier);

        return TestBookings.withId(11L, listing, customer, START, END);
    }
}
