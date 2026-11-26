/**
 * This program proves the BO-01 HTTP surface: list is public, create
 * and delete use the same JWT AuthenticatedUser as /api/auth/me, and
 * a non-supplier or a service-reported failure maps to the right
 * status code.
 *
 * File:   BlackoutControllerTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.TestBlackouts;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.exception.BlackoutNotFoundException;
import com.nicheflow.exception.GlobalExceptionHandler;
import com.nicheflow.exception.ListingAccessDeniedException;
import com.nicheflow.model.Listing;
import com.nicheflow.model.ListingBlackout;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.JwtService;
import com.nicheflow.service.ListingBlackoutService;

@WebMvcTest(BlackoutController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class BlackoutControllerTest {

    private static final Instant START = Instant.parse(
            "2026-11-01T00:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-05T00:00:00Z");
    private static final String CREATE_BODY = """
            {"startTime":"2026-11-01T00:00:00Z",\
            "endTime":"2026-11-05T00:00:00Z","reason":"Vacation"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingBlackoutService blackoutService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * createPassesTheJwtPrincipalIntoTheService - This function
     * checks ListingBlackoutService.create receives the caller
     * parsed off the Bearer token.
     */
    @Test
    void createPassesTheJwtPrincipalIntoTheService() throws Exception {
        AuthenticatedUser caller = supplier(7L, "good-token");
        ListingBlackout stored = storedBlackout();
        given(blackoutService.create(eq(caller), eq(3L), eq(START),
                eq(END), eq("Vacation"))).willReturn(stored);

        mockMvc.perform(post("/api/listings/3/blackouts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.listingId").value(3))
                .andExpect(jsonPath("$.reason").value("Vacation"));

        verify(blackoutService).create(eq(caller), eq(3L), eq(START),
                eq(END), eq("Vacation"));
    }

    /**
     * createWithoutATokenReturns401 - This function hits create with
     * no Authorization header, the same 401 path as /me.
     */
    @Test
    void createWithoutATokenReturns401() throws Exception {
        mockMvc.perform(post("/api/listings/3/blackouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isUnauthorized());
    }

    /**
     * createWithACustomerTokenReturns403 - This function sends a
     * CUSTOMER JWT to create and expects the ROLE_SUPPLIER gate.
     */
    @Test
    void createWithACustomerTokenReturns403() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(post("/api/listings/3/blackouts")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isForbidden());

        verify(blackoutService, never()).create(any(), any(), any(),
                any(), any());
    }

    /**
     * createWithAMissingBodyFieldReturns400 - This function omits
     * endTime so bean validation is proven on create.
     */
    @Test
    void createWithAMissingBodyFieldReturns400() throws Exception {
        supplier(7L, "good-token");

        mockMvc.perform(post("/api/listings/3/blackouts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startTime":"2026-11-01T00:00:00Z"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.endTime").exists());

        verify(blackoutService, never()).create(any(), any(), any(),
                any(), any());
    }

    /**
     * createWhenTheServiceDeniesOwnershipReturns403 - This function
     * stubs another supplier's JWT and a service ownership failure.
     */
    @Test
    void createWhenTheServiceDeniesOwnershipReturns403() throws Exception {
        AuthenticatedUser other = supplier(8L, "other-token");
        given(blackoutService.create(eq(other), eq(3L), any(), any(),
                any()))
                .willThrow(new ListingAccessDeniedException());

        mockMvc.perform(post("/api/listings/3/blackouts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer other-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    /**
     * listWithoutATokenReturnsStoredBlackouts - This function proves
     * visitors can read blackouts without signing in.
     */
    @Test
    void listWithoutATokenReturnsStoredBlackouts() throws Exception {
        given(blackoutService.listForListing(3L))
                .willReturn(List.of(storedBlackout()));

        mockMvc.perform(get("/api/listings/3/blackouts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].listingId").value(3));
    }

    /**
     * deletePassesTheJwtPrincipalAndReturns204 - This function
     * checks delete uses the same principal as create.
     */
    @Test
    void deletePassesTheJwtPrincipalAndReturns204() throws Exception {
        AuthenticatedUser caller = supplier(7L, "good-token");

        mockMvc.perform(delete("/api/listings/3/blackouts/5")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isNoContent());

        verify(blackoutService).delete(caller, 3L, 5L);
    }

    /**
     * deleteWhenTheServiceReportsNotFoundReturns404 - This function
     * stubs a missing blackout and expects HTTP 404.
     */
    @Test
    void deleteWhenTheServiceReportsNotFoundReturns404() throws Exception {
        AuthenticatedUser caller = supplier(7L, "good-token");
        org.mockito.BDDMockito.willThrow(new BlackoutNotFoundException(5L))
                .given(blackoutService).delete(caller, 3L, 5L);

        mockMvc.perform(delete("/api/listings/3/blackouts/5")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isNotFound());
    }

    /**
     * deleteWithACustomerTokenReturns403 - This function sends a
     * CUSTOMER JWT to delete and expects the ROLE_SUPPLIER gate.
     */
    @Test
    void deleteWithACustomerTokenReturns403() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(delete("/api/listings/3/blackouts/5")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isForbidden());

        verify(blackoutService, never()).delete(any(), any(), any());
    }

    /**
     * storedBlackout - This function builds the blackout the mocked
     * service returns for create and list assertions.
     *
     * Return - A blackout with id 5 on listing 3
     */
    private static ListingBlackout storedBlackout() {
        User owner = TestUsers.withId(7L, "ada@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        Listing listing = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), owner);
        return TestBlackouts.withId(5L, listing, START, END, "Vacation");
    }

    /**
     * supplier - This function stubs jwtService.parse the same way
     * ListingControllerTest stubs it so the filter places an
     * AuthenticatedUser in the security context.
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
