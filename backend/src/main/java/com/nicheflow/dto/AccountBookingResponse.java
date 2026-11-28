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
        BigDecimal refundAmount,
        Double customerAverageRating,
        long customerReviewCount,
        boolean reviewedByCaller) {

    /**
     * from - This function copies the public booking fields for a
     * customer's own account view, with no ADM-02 customer standing
     * attached since that is only shown to the listing's supplier.
     *
     * Parameters:
     * booking - The stored booking to project
     * reviewedByCaller - Whether the JWT caller has already left a
     *     review on this booking, already looked up in bulk so the
     *     account page need not probe each booking individually
     */
    public static AccountBookingResponse from(Booking booking,
            boolean reviewedByCaller) {
        return from(booking, null, 0L, reviewedByCaller);
    }

    /**
     * from - This function copies the public booking fields, plus
     * the customer's already-summarized ADM-02 standing: the average
     * rating and count of reviews suppliers have left about them,
     * shown to the listing's supplier so they can judge a request
     * (including a still-PENDING one) before accepting it.
     *
     * Parameters:
     * booking - The stored booking to project
     * customerAverageRating - The mean of every rating this
     *     customer has received, or null when they have none
     * customerReviewCount - How many reviews were averaged
     * reviewedByCaller - Whether the JWT caller has already left a
     *     review on this booking
     */
    public static AccountBookingResponse from(Booking booking,
            Double customerAverageRating, long customerReviewCount,
            boolean reviewedByCaller) {
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
                booking.getRefundAmount(),
                customerAverageRating,
                customerReviewCount,
                reviewedByCaller);
    }
}
