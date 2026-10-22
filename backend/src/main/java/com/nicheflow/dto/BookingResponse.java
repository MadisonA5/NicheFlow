/**
 * This program is the public booking payload returned after BK-01
 * create and cancel. It exposes listing and customer ids, not the
 * User or Listing rows, so a password hash never leaves the API.
 * refundAmount is null until the booking is cancelled.
 *
 * File:   BookingResponse.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.nicheflow.model.Booking;

public record BookingResponse(Long id, Long listingId, Long customerId,
        String status, Instant startTime, Instant endTime,
        BigDecimal refundAmount) {

    /**
     * from - This function copies the public booking fields. Status
     * is the enum name, which starts as PENDING on create.
     *
     * Parameters:
     * booking - The stored booking to project
     *
     * Return - The public booking payload
     */
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(booking.getId(),
                booking.getListing().getId(),
                booking.getCustomer().getId(),
                booking.getStatus().name(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getRefundAmount());
    }
}
