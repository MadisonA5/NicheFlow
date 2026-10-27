package com.nicheflow.dto;

import java.time.Instant;

import com.nicheflow.model.Booking;

public record BookingAvailabilityResponse(Instant startTime,
        Instant endTime) {

    public static BookingAvailabilityResponse from(Booking booking) {
        return new BookingAvailabilityResponse(booking.getStartTime(),
                booking.getEndTime());
    }
}
