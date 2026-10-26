/**
 * This program is the listing HTTP surface: LST-01 create, edit, and
 * deactivate, plus LST-02 search. Write callers are injected with
 * @AuthenticationPrincipal AuthenticatedUser, the same way
 * AuthController /api/auth/me reads the JWT.
 *
 * File:   ListingController.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.controller;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.ListingPageResponse;
import com.nicheflow.dto.ListingRequest;
import com.nicheflow.dto.ListingResponse;
import com.nicheflow.model.Listing;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.ListingService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    /**
     * ListingController - This function stores the listing service
     * Spring injects so each endpoint can stay thin.
     *
     * Parameters:
     * listingService - The service that creates, edits, and hides
     *     listings
     */
    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    /**
     * create - This function opens a listing for the JWT caller. The
     * principal is the same AuthenticatedUser /me returns.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * request - The title, category, price, location, and description
     *
     * Return - The persisted listing
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @Valid @RequestBody ListingRequest request) {
        // To persist the listing owned by this JWT subject
        Listing listing = listingService.create(caller, request.title(),
                request.category(), request.price(), request.location(),
                request.description());

        return ListingResponse.from(listing);
    }

    /**
     * search - This function returns one page of active listings
     * filtered by optional keyword, category, and price range.
     * Visitors may browse without a login. Writes stay
     * supplier-only.
     *
     * Parameters:
     * keyword - Title fragment, case-insensitive, or omitted
     * category - Exact category, or omitted
     * minPrice - Inclusive lower price matching ListingRequest
     *     rules, or omitted
     * maxPrice - Inclusive upper price matching ListingRequest
     *     rules, or omitted
     * page - Zero-based page number, or omitted for 0
     * size - Page size, or omitted for 20
     *
     * Return - The page of public listing payloads
     */
    @GetMapping
    public ListingPageResponse search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false)
            @DecimalMin(value = "0.00", inclusive = true)
            @Digits(integer = 8, fraction = 2)
            BigDecimal minPrice,
            @RequestParam(required = false)
            @DecimalMin(value = "0.00", inclusive = true)
            @Digits(integer = 8, fraction = 2)
            BigDecimal maxPrice,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // To map the page the service already filtered and sliced
        Page<Listing> result = listingService.search(keyword, category,
                minPrice, maxPrice, page, size);

        return ListingPageResponse.from(result);
    }

    /**
     * update - This function edits a listing when the JWT caller is
     * its supplier. Any other authenticated user is 403.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * listingId - The listing to edit; must be 1 or greater
     * request - The new title, category, price, location, and
     *     description
     *
     * Return - The updated listing
     */
    @PutMapping("/{listingId}")
    public ListingResponse update(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long listingId,
            @Valid @RequestBody ListingRequest request) {
        // To overwrite fields only after the service checks ownership
        Listing listing = listingService.update(caller, listingId,
                request.title(), request.category(), request.price(),
                request.location(), request.description());

        return ListingResponse.from(listing);
    }

    /**
     * deactivate - This function sets active to false when the JWT
     * caller owns the listing. The row is not deleted.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * listingId - The listing to hide; must be 1 or greater
     *
     * Return - The listing with active false
     */
    @PostMapping("/{listingId}/deactivate")
    public ListingResponse deactivate(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long listingId) {
        // To hide the listing using the same JWT principal as /me
        Listing listing = listingService.deactivate(caller, listingId);

        return ListingResponse.from(listing);
    }
}
