/**
 * This program is the LST-01 HTTP surface: create, edit, and
 * deactivate a listing. The caller is injected with
 * @AuthenticationPrincipal AuthenticatedUser, the same way
 * AuthController /api/auth/me reads the JWT.
 *
 * File:   ListingController.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.ListingRequest;
import com.nicheflow.dto.ListingResponse;
import com.nicheflow.model.Listing;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.ListingService;

import jakarta.validation.Valid;

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
     * request - The title, category, and price
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
                request.category(), request.price());

        return ListingResponse.from(listing);
    }

    /**
     * update - This function edits a listing when the JWT caller is
     * its supplier. Any other authenticated user is 403.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * listingId - The listing to edit
     * request - The new title, category, and price
     *
     * Return - The updated listing
     */
    @PutMapping("/{listingId}")
    public ListingResponse update(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable Long listingId,
            @Valid @RequestBody ListingRequest request) {
        // To overwrite fields only after the service checks ownership
        Listing listing = listingService.update(caller, listingId,
                request.title(), request.category(), request.price());

        return ListingResponse.from(listing);
    }

    /**
     * deactivate - This function sets active to false when the JWT
     * caller owns the listing. The row is not deleted.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * listingId - The listing to hide
     *
     * Return - The listing with active false
     */
    @PostMapping("/{listingId}/deactivate")
    public ListingResponse deactivate(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable Long listingId) {
        // To hide the listing using the same JWT principal as /me
        Listing listing = listingService.deactivate(caller, listingId);

        return ListingResponse.from(listing);
    }
}
