/**
 * This program proves the LST-01 HTTP surface uses the same JWT
 * AuthenticatedUser as /api/auth/me: the filter parses the token,
 * @AuthenticationPrincipal injects that principal, and a second
 * supplier or a customer gets 403.
 *
 * File:   ListingControllerTest.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.controller;

import java.math.BigDecimal;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.exception.GlobalExceptionHandler;
import com.nicheflow.exception.ListingAccessDeniedException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.JwtService;
import com.nicheflow.service.ListingService;

@WebMvcTest(ListingController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ListingControllerTest {

    private static final String CREATE_BODY = """
            {"title":"Kayak","category":"SPORTS","price":25.00}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingService listingService;

    @MockitoBean
    private JwtService jwtService;

    /**
     * createPassesTheJwtPrincipalIntoTheService - This function
     * stubs jwtService.parse the same way /me tests do and checks
     * ListingService.create receives that AuthenticatedUser.
     */
    @Test
    void createPassesTheJwtPrincipalIntoTheService() throws Exception {
        AuthenticatedUser caller = supplier(7L, "good-token");
        User owner = TestUsers.withId(7L, "ada@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        Listing stored = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), owner);
        given(listingService.create(eq(caller), eq("Kayak"), eq("SPORTS"),
                any())).willReturn(stored);

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.supplierId").value(7))
                .andExpect(jsonPath("$.active").value(true));

        verify(listingService).create(eq(caller), eq("Kayak"),
                eq("SPORTS"), any());
    }

    /**
     * createWithoutATokenReturns401 - This function hits create with
     * no Authorization header, the same 401 path as /me.
     */
    @Test
    void createWithoutATokenReturns401() throws Exception {
        mockMvc.perform(post("/api/listings")
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

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isForbidden());

        verify(listingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * updateWithANegativePriceReturns400 - This function sends an
     * invalid price so bean validation is proven on the edit path.
     */
    @Test
    void updateWithANegativePriceReturns400() throws Exception {
        supplier(7L, "good-token");

        mockMvc.perform(put("/api/listings/3")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Kayak","category":"SPORTS","price":-1.00}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    /**
     * updateWhenTheServiceDeniesOwnershipReturns403 - This function
     * stubs another supplier's JWT and a service ownership failure.
     */
    @Test
    void updateWhenTheServiceDeniesOwnershipReturns403() throws Exception {
        AuthenticatedUser other = supplier(8L, "other-token");
        given(listingService.update(eq(other), eq(3L), any(), any(),
                any())).willThrow(new ListingAccessDeniedException());

        mockMvc.perform(put("/api/listings/3")
                .header(HttpHeaders.AUTHORIZATION, "Bearer other-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    /**
     * updateWhenTheListingIsMissingReturns404 - This function stubs
     * a missing id so the client gets 404.
     */
    @Test
    void updateWhenTheListingIsMissingReturns404() throws Exception {
        AuthenticatedUser caller = supplier(7L, "good-token");
        given(listingService.update(eq(caller), eq(99L), any(), any(),
                any())).willThrow(new ListingNotFoundException(99L));

        mockMvc.perform(put("/api/listings/99")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isNotFound());
    }

    /**
     * deactivatePassesTheJwtPrincipalAndReturnsActiveFalse - This
     * function checks deactivate uses the same principal and does
     * not hard-delete.
     */
    @Test
    void deactivatePassesTheJwtPrincipalAndReturnsActiveFalse()
            throws Exception {
        AuthenticatedUser caller = supplier(7L, "good-token");
        User owner = TestUsers.withId(7L, "ada@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        Listing hidden = TestListings.withId(3L, "Kayak", "SPORTS",
                new BigDecimal("25.00"), owner);
        hidden.setActive(false);
        given(listingService.deactivate(caller, 3L)).willReturn(hidden);

        mockMvc.perform(post("/api/listings/3/deactivate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.id").value(3));

        verify(listingService).deactivate(caller, 3L);
    }

    /**
     * supplier - This function stubs jwtService.parse the same way
     * AuthControllerTest stubs /me so the filter places an
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
