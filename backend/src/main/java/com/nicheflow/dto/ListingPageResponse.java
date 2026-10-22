/**
 * This program is the LST-02 search payload: one page of public
 * listings plus the page number, size, and totals the client needs
 * to walk the result set.
 *
 * File:   ListingPageResponse.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.nicheflow.model.Listing;

public record ListingPageResponse(List<ListingResponse> items, int page,
        int size, long totalElements, int totalPages) {

    /**
     * from - This function maps a Spring page of listings into the
     * public search body.
     *
     * Parameters:
     * result - The page returned by the search query
     *
     * Return - The items plus pagination fields
     */
    public static ListingPageResponse from(Page<Listing> result) {
        List<ListingResponse> items = result.getContent().stream()
                .map(ListingResponse::from)
                .toList();

        return new ListingPageResponse(items, result.getNumber(),
                result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }
}
