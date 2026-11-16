/**
 * This program computes what a booking costs. A listing price is a
 * price per day (the frontend shows "/ day" and estimates price times
 * days), so a booking costs the price times the number of days it
 * covers. A part day counts as a whole day, and every booking counts
 * as at least one day, so a one-hour slot costs one day.
 *
 * File:   BookingPriceCalculator.java
 * Course: AUCSC 325
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

public final class BookingPriceCalculator {

    private static final long SECONDS_PER_DAY = Duration.ofDays(1)
            .toSeconds();

    /**
     * BookingPriceCalculator - This function hides the constructor.
     * Callers use totalPrice.
     */
    private BookingPriceCalculator() {
    }

    /**
     * totalPrice - This function returns the price of a booking from
     * startTime to endTime at the given price per day.
     *
     * Parameters:
     * pricePerDay - The listing price
     * startTime - The UTC instant the booking starts
     * endTime - The UTC instant the booking ends, after startTime
     *
     * Return - The total, with scale 2
     *
     * Example:
     * Price 50.00, 2027-01-10T00:00:00Z to 2027-01-15T00:00:00Z
     * returns 250.00.
     */
    public static BigDecimal totalPrice(BigDecimal pricePerDay,
            Instant startTime, Instant endTime) {
        return pricePerDay.multiply(BigDecimal.valueOf(days(startTime,
                endTime))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * days - This function counts the days a booking covers, rounding
     * a part day up and never returning less than one.
     *
     * Parameters:
     * startTime - The UTC instant the booking starts
     * endTime - The UTC instant the booking ends
     *
     * Return - The number of days to charge
     */
    static long days(Instant startTime, Instant endTime) {
        long seconds = Duration.between(startTime, endTime).toSeconds();
        long wholeDays = Math.ceilDiv(seconds, SECONDS_PER_DAY);
        return Math.max(1, wholeDays);
    }
}
