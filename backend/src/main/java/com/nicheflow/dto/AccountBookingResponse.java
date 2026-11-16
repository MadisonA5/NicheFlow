package com.nicheflow.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.nicheflow.model.Booking;
import com.nicheflow.model.PriceUnit;
import com.nicheflow.service.BookingPriceCalculator;

public record AccountBookingResponse(
        Long id,
        Long listingId,
        String listingTitle,
        String location,
        BigDecimal price,
        PriceUnit priceUnit,
        BigDecimal totalPrice,
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
                booking.getListing().getPriceUnit(),
                BookingPriceCalculator.total(
                        booking.getListing().getPrice(),
                        booking.getListing().getPriceUnit(),
                        booking.getStartTime(), booking.getEndTime()),
                booking.getCustomer().getId(),
                booking.getStatus().name(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getRefundAmount());
    }
}
