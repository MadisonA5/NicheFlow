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
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.exception.GlobalExceptionHandler;
import com.nicheflow.exception.InvalidSearchException;
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
     * searchAllowsACustomerAndPassesTheQueryFilters - This function
     * proves browse is not supplier-only and forwards keyword and
     * category to the service.
     */
    @Test
    void searchAllowsACustomerAndPassesTheQueryFilters() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));
        User owner = TestUsers.withId(7L, "ada@example.com",
                "$2a$10$hashed", Role.SUPPLIER);
        Listing hit = TestListings.withId(3L, "Red Kayak", "SPORTS",
                new BigDecimal("25.00"), owner);
        given(listingService.search("kayak", "SPORTS", null, null, null,
                null)).willReturn(new PageImpl<>(List.of(hit)));

        mockMvc.perform(get("/api/listings")
                .param("keyword", "kayak")
                .param("category", "SPORTS")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Red Kayak"))
                .andExpect(jsonPath("$.items[0].active").value(true));

        verify(listingService).search("kayak", "SPORTS", null, null,
                null, null);
    }

    /**
     * searchWithNoHitsReturnsAnEmptyArray - This function covers the
     * LST-02 empty-results case with 200 and [].
     */
    @Test
    void searchWithNoHitsReturnsAnEmptyArray() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));
        given(listingService.search(null, null, null, null, null, null))
                .willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/listings")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    /**
     * searchWithoutATokenReturns401 - This function hits browse with
     * no Authorization header, the same 401 path as /me.
     */
    @Test
    void searchWithoutATokenReturns401() throws Exception {
        mockMvc.perform(get("/api/listings"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * searchPassesPriceAndPageParams - This function forwards
     * minPrice, maxPrice, page, and size on top of keyword and
     * category.
     */
    @Test
    void searchPassesPriceAndPageParams() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));
        given(listingService.search(eq("kayak"), eq("SPORTS"),
                eq(new BigDecimal("10.00")), eq(new BigDecimal("50.00")),
                eq(2), eq(5))).willReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/listings")
                .param("keyword", "kayak")
                .param("category", "SPORTS")
                .param("minPrice", "10.00")
                .param("maxPrice", "50.00")
                .param("page", "2")
                .param("size", "5")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isOk());

        verify(listingService).search(eq("kayak"), eq("SPORTS"),
                eq(new BigDecimal("10.00")), eq(new BigDecimal("50.00")),
                eq(2), eq(5));
    }

    /**
     * searchWithInvertedPriceRangeReturns400 - This function stubs
     * minPrice greater than maxPrice so the client gets 400.
     */
    @Test
    void searchWithInvertedPriceRangeReturns400() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));
        given(listingService.search(isNull(), isNull(),
                eq(new BigDecimal("50.00")), eq(new BigDecimal("10.00")),
                isNull(), isNull()))
                .willThrow(new InvalidSearchException(
                        "minPrice cannot be greater than maxPrice"));

        mockMvc.perform(get("/api/listings")
                .param("minPrice", "50.00")
                .param("maxPrice", "10.00")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isBadRequest());
    }

    /**
     * createWithABlankTitleReturns400 - This function sends an empty
     * title so LST-01 input validation is proven on create.
     */
    @Test
    void createWithABlankTitleReturns400() throws Exception {
        supplier(7L, "good-token");

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"","category":"SPORTS","price":25.00}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verify(listingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * createWithAnOversizedTitleReturns400 - This function sends a
     * 256-character title so the 255-character cap is proven.
     */
    @Test
    void createWithAnOversizedTitleReturns400() throws Exception {
        supplier(7L, "good-token");
        String oversizedTitle = "A".repeat(256);

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","category":"SPORTS","price":25.00}
                        """.formatted(oversizedTitle)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists());

        verify(listingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * createWithABlankCategoryReturns400 - This function sends an
     * empty category so LST-01 input validation is proven on create.
     */
    @Test
    void createWithABlankCategoryReturns400() throws Exception {
        supplier(7L, "good-token");

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Kayak","category":"","price":25.00}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").exists());

        verify(listingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * createWithAnOversizedCategoryReturns400 - This function sends a
     * 256-character category so the 255-character cap is proven.
     */
    @Test
    void createWithAnOversizedCategoryReturns400() throws Exception {
        supplier(7L, "good-token");
        String oversizedCategory = "A".repeat(256);

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Kayak","category":"%s","price":25.00}
                        """.formatted(oversizedCategory)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.category").exists());

        verify(listingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * createWithMoreThanTwoPriceDecimalsReturns400 - This function
     * sends 25.123 so @Digits on ListingRequest is proven.
     */
    @Test
    void createWithMoreThanTwoPriceDecimalsReturns400() throws Exception {
        supplier(7L, "good-token");

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Kayak","category":"SPORTS","price":25.123}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());

        verify(listingService, never()).create(any(), any(), any(),
                any());
    }

    /**
     * updateWithANegativeListingIdReturns400 - This function hits
     * edit with listingId -1 so a non-positive id never reaches the
     * service.
     */
    @Test
    void updateWithANegativeListingIdReturns400() throws Exception {
        supplier(7L, "good-token");

        mockMvc.perform(put("/api/listings/-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.listingId").exists());

        verify(listingService, never()).update(any(), any(), any(),
                any(), any());
    }

    /**
     * deactivateWithANegativeListingIdReturns400 - This function hits
     * deactivate with listingId -1 so a non-positive id never reaches
     * the service.
     */
    @Test
    void deactivateWithANegativeListingIdReturns400() throws Exception {
        supplier(7L, "good-token");

        mockMvc.perform(post("/api/listings/-1/deactivate")
                .header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.listingId").exists());

        verify(listingService, never()).deactivate(any(), any());
    }

    /**
     * searchWithANegativeMinPriceReturns400 - This function sends
     * minPrice=-1 so search matches ListingRequest's floor.
     */
    @Test
    void searchWithANegativeMinPriceReturns400() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(get("/api/listings")
                .param("minPrice", "-1.00")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.minPrice").exists());

        verify(listingService, never()).search(any(), any(), any(),
                any(), any(), any());
    }

    /**
     * searchWithANegativeMaxPriceReturns400 - This function sends
     * maxPrice=-1 so the upper bound matches ListingRequest.
     */
    @Test
    void searchWithANegativeMaxPriceReturns400() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(get("/api/listings")
                .param("maxPrice", "-1.00")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxPrice").exists());

        verify(listingService, never()).search(any(), any(), any(),
                any(), any(), any());
    }

    /**
     * searchWithAMalformedMinPriceReturns400 - This function sends
     * minPrice=foo so a non-number is 400, not a 500.
     */
    @Test
    void searchWithAMalformedMinPriceReturns400() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(get("/api/listings")
                .param("minPrice", "foo")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.minPrice").exists());

        verify(listingService, never()).search(any(), any(), any(),
                any(), any(), any());
    }

    /**
     * searchWithAMalformedMaxPriceReturns400 - This function sends
     * maxPrice=bar so a non-number is 400, not a 500.
     */
    @Test
    void searchWithAMalformedMaxPriceReturns400() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(get("/api/listings")
                .param("maxPrice", "bar")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.maxPrice").exists());

        verify(listingService, never()).search(any(), any(), any(),
                any(), any(), any());
    }

    /**
     * searchPastTheLastPageReturnsEmptyItemsAndTotalElements - This
     * function stubs page 5 of size 20 when 25 rows exist so items
     * is empty and totalElements stays 25.
     */
    @Test
    void searchPastTheLastPageReturnsEmptyItemsAndTotalElements()
            throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "bob@example.com",
                        Role.CUSTOMER));
        given(listingService.search(null, null, null, null, 5, 20))
                .willReturn(new PageImpl<>(List.of(),
                        PageRequest.of(5, 20), 25));

        mockMvc.perform(get("/api/listings")
                .param("page", "5")
                .param("size", "20")
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.page").value(5));
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
