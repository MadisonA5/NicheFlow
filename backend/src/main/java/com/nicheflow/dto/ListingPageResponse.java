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
import java.util.Map;

import org.springframework.data.domain.Page;

import com.nicheflow.model.Listing;
import com.nicheflow.repository.ReviewRepository.ListingRatingSummary;

public record ListingPageResponse(List<ListingResponse> items, int page,
        int size, long totalElements, int totalPages) {

    /**
     * from - This function maps a Spring page of listings into the
     * public search body, with no ADM-02 rating data attached.
     *
     * Parameters:
     * result - The page returned by the search query
     *
     * Return - The items plus pagination fields
     */
    public static ListingPageResponse from(Page<Listing> result) {
        return from(result, Map.of());
    }

    /**
     * from - This function maps a Spring page of listings into the
     * public search body, attaching each listing's already-summarized
     * ADM-02 average rating and review count.
     *
     * Parameters:
     * result - The page returned by the search query
     * ratingSummaries - The rating summary for each listing id that
     *     has at least one review
     *
     * Return - The items plus pagination fields
     */
    public static ListingPageResponse from(Page<Listing> result,
            Map<Long, ListingRatingSummary> ratingSummaries) {
        List<ListingResponse> items = result.getContent().stream()
                .map(listing -> ListingResponse.from(listing,
                        ratingSummaries))
                .toList();

        return new ListingPageResponse(items, result.getNumber(),
                result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }
}
