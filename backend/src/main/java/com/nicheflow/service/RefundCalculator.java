/**
 * This program computes the BK-04 refund from the listing price and
 * how early the booking was cancelled. A cancellation 24 hours or
 * more before the start returns the full price. A cancellation
 * later than that, but still before the start, returns half. A
 * cancellation at or after the start returns zero.
 *
 * File:   RefundCalculator.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class RefundCalculator {

    private static final long FULL_REFUND_HOURS = 24;

    private static final BigDecimal HALF = new BigDecimal("0.50");

    /**
     * RefundCalculator - This function hides the constructor. Callers
     * use calculateRefund.
     */
    private RefundCalculator() {
    }

    /**
     * calculateRefund - This function returns the refund for a
     * cancellation. The exact instant 24 hours before the start is
     * a full refund. One nanosecond later, still before the start,
     * is half, rounded half up to two decimal places.
     *
     * Parameters:
     * price - The listing price being refunded
     * startTime - The UTC instant the booking starts
     * cancelledAt - The UTC instant the booking is cancelled
     *
     * Return - The refund, with scale 2
     *
     * Example:
     * Price 10.00, start 2026-11-02T15:00:00Z, cancelled
     * 2026-11-01T15:00:00Z returns 10.00.
     */
    public static BigDecimal calculateRefund(BigDecimal price,
            Instant startTime, Instant cancelledAt) {
        Instant fullRefundCutoff = startTime.minus(FULL_REFUND_HOURS,
                ChronoUnit.HOURS);

        // To pay nothing once the slot has started
        if (!cancelledAt.isBefore(startTime)) {
            return zero();
        }

        // To pay the full price at or before the 24 hour cutoff
        if (!cancelledAt.isAfter(fullRefundCutoff)) {
            return money(price);
        }

        // To pay half when the cancel falls inside the last 24 hours
        return money(price.multiply(HALF));
    }

    /**
     * money - This function rounds a refund to two decimal places.
     *
     * Parameters:
     * amount - The unscaled refund
     *
     * Return - The amount at scale 2, half up
     */
    private static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * zero - This function returns a refund of 0.00.
     *
     * Return - Zero at scale 2
     */
    private static BigDecimal zero() {
        return money(BigDecimal.ZERO);
    }
}
