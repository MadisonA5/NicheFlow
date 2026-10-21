/**
 * This program is the ADM-01 bookings payload: one page of the
 * existing booking responses plus the page number, size, and totals
 * the admin console needs to walk the result set.
 *
 * File:   AdminBookingPageResponse.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.nicheflow.model.Booking;

public record AdminBookingPageResponse(List<BookingResponse> items,
        int page, int size, long totalElements, int totalPages) {

    /**
     * from - This function maps a Spring page of bookings into the
     * admin list body. Each item is the same BookingResponse the
     * booking routes already return.
     *
     * Parameters:
     * result - The page returned by the booking query
     *
     * Return - The items plus pagination fields
     */
    public static AdminBookingPageResponse from(Page<Booking> result) {
        List<BookingResponse> items = result.getContent().stream()
                .map(BookingResponse::from)
                .toList();

        return new AdminBookingPageResponse(items, result.getNumber(),
                result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }
}
