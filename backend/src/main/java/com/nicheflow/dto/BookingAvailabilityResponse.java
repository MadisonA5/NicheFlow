package com.nicheflow.dto;

import java.time.Instant;

import com.nicheflow.model.Booking;
import com.nicheflow.model.ListingBlackout;

public record BookingAvailabilityResponse(Instant startTime,
        Instant endTime) {

    public static BookingAvailabilityResponse from(Booking booking) {
        return new BookingAvailabilityResponse(booking.getStartTime(),
                booking.getEndTime());
    }

    public static BookingAvailabilityResponse from(
            ListingBlackout blackout) {
        return new BookingAvailabilityResponse(blackout.getStartTime(),
                blackout.getEndTime());
    }
}
