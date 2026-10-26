package com.nicheflow.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.nicheflow.model.Booking;

public record AccountBookingResponse(
        Long id,
        Long listingId,
        String listingTitle,
        String location,
        BigDecimal price,
        Long customerId,
        String status,
        Instant startTime,
        Instant endTime,
        BigDecimal refundAmount) {

    public static AccountBookingResponse from(Booking booking) {
        return new AccountBookingResponse(
                booking.getId(),
                booking.getListing().getId(),
                booking.getListing().getTitle(),
                booking.getListing().getLocation(),
                booking.getListing().getPrice(),
                booking.getCustomer().getId(),
                booking.getStatus().name(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getRefundAmount());
    }
}
