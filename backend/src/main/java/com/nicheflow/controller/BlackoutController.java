/**
 * This program is the blackout HTTP surface for BO-01. Create and
 * delete are supplier-owner-only; list is public, the same way
 * booking availability is public, since a blackout window is not
 * sensitive once it is folded into that same availability view.
 *
 * File:   BlackoutController.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.nicheflow.dto.BlackoutRequest;
import com.nicheflow.dto.BlackoutResponse;
import com.nicheflow.model.ListingBlackout;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.ListingBlackoutService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api/listings/{listingId}/blackouts")
public class BlackoutController {

    private final ListingBlackoutService blackoutService;

    /**
     * BlackoutController - This function stores the blackout service
     * Spring injects so each endpoint can stay thin.
     *
     * Parameters:
     * blackoutService - The service that creates, lists, and deletes
     *     listing blackouts
     */
    public BlackoutController(ListingBlackoutService blackoutService) {
        this.blackoutService = blackoutService;
    }

    /**
     * list - This function returns every blackout stored for a
     * listing. Visitors may browse without a login.
     *
     * Parameters:
     * listingId - The listing whose blackouts are requested
     *
     * Return - The blackouts for that listing, possibly empty
     */
    @GetMapping
    public List<BlackoutResponse> list(
            @PathVariable @Min(1) Long listingId) {
        return blackoutService.listForListing(listingId).stream()
                .map(BlackoutResponse::from)
                .toList();
    }

    /**
     * create - This function adds a blackout to a listing the JWT
     * caller owns.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * listingId - The listing to black out; must be 1 or greater
     * request - The UTC window and optional reason
     *
     * Return - The persisted blackout
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BlackoutResponse create(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long listingId,
            @Valid @RequestBody BlackoutRequest request) {
        ListingBlackout blackout = blackoutService.create(caller,
                listingId, request.startTime(), request.endTime(),
                request.reason());

        return BlackoutResponse.from(blackout);
    }

    /**
     * delete - This function removes a blackout from a listing the
     * JWT caller owns.
     *
     * Parameters:
     * caller - The identity parsed out of a valid JWT
     * listingId - The listing the blackout is on; must be 1 or
     *     greater
     * blackoutId - The blackout to remove; must be 1 or greater
     */
    @DeleteMapping("/{blackoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedUser caller,
            @PathVariable @Min(1) Long listingId,
            @PathVariable @Min(1) Long blackoutId) {
        blackoutService.delete(caller, listingId, blackoutId);
    }
}
