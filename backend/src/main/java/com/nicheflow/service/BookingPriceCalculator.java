/**
 * This program computes what a booking costs for issue #14. The
 * listing price is charged once for every started day or hour of the
 * booking window, depending on the listing's price unit. Confirm
 * charges this total and cancel refunds against it, so the customer
 * pays what the booking page estimated.
 *
 * File:   BookingPriceCalculator.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import com.nicheflow.model.PriceUnit;

public final class BookingPriceCalculator {

    /**
     * BookingPriceCalculator - This function hides the constructor.
     * Callers use units and total.
     */
    private BookingPriceCalculator() {
    }

    /**
     * units - This function counts the started units in a window. A
     * partial unit counts as a whole one.
     *
     * Parameters:
     * unit - What one listing price buys
     * startTime - The UTC instant the booking starts
     * endTime - The UTC instant the booking ends, after startTime
     *
     * Return - The number of units, at least 1
     *
     * Example:
     * HOUR from 10:00Z to 12:30Z is 3. DAY from 2026-11-13T22:00Z to
     * 2026-11-14T18:00Z is 1.
     */
    public static long units(PriceUnit unit, Instant startTime,
            Instant endTime) {
        Duration window = Duration.between(startTime, endTime);
        long unitNanos = unit.length().toNanos();

        // To round a partial unit up to a whole one
        return Math.max(1,
                (window.toNanos() + unitNanos - 1) / unitNanos);
    }

    /**
     * total - This function multiplies the listing price by the
     * started units in the window.
     *
     * Parameters:
     * price - The listing price for one unit
     * unit - What one listing price buys
     * startTime - The UTC instant the booking starts
     * endTime - The UTC instant the booking ends, after startTime
     *
     * Return - The total, with scale 2
     *
     * Example:
     * Price 12.50 per HOUR from 10:00Z to 12:00Z returns 25.00.
     */
    public static BigDecimal total(BigDecimal price, PriceUnit unit,
            Instant startTime, Instant endTime) {
        return price.multiply(BigDecimal.valueOf(
                        units(unit, startTime, endTime)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
