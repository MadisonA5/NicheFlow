package com.nicheflow.dto;

import java.util.List;

public record AccountResponse(
        List<ListingResponse> listings,
        List<AccountBookingResponse> bookings) {
}
