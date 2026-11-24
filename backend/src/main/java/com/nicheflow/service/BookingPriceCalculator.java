/**
 * This program computes a booking's cost from its hourly listing rate.
 * A partial hour is rounded up, and every booking is at least one hour.
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

    private static final long SECONDS_PER_HOUR = Duration.ofHours(1)
            .toSeconds();

    /**
     * BookingPriceCalculator - This function hides the constructor.
     * Callers use totalPrice.
     */
    private BookingPriceCalculator() {
    }

    /**
     * totalPrice - This function returns the price of a booking from
     * startTime to endTime at the given price per hour.
     *
     * Parameters:
     * pricePerHour - The listing price
     * startTime - The UTC instant the booking starts
     * endTime - The UTC instant the booking ends, after startTime
     *
     * Return - The total, with scale 2
     *
     * Example:
     * Price 50.00, 2027-01-10T00:00:00Z to 2027-01-10T03:00:00Z
     * returns 150.00.
     */
    public static BigDecimal totalPrice(BigDecimal pricePerHour,
            Instant startTime, Instant endTime) {
        return pricePerHour.multiply(BigDecimal.valueOf(hours(startTime,
                endTime))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * hours - This function counts the hours a booking covers, rounding
     * a part hour up and never returning less than one.
     *
     * Parameters:
     * startTime - The UTC instant the booking starts
     * endTime - The UTC instant the booking ends
     *
     * Return - The number of hours to charge
     */
    static long hours(Instant startTime, Instant endTime) {
        long seconds = Duration.between(startTime, endTime).toSeconds();
        long wholeHours = Math.ceilDiv(seconds, SECONDS_PER_HOUR);
        return Math.max(1, wholeHours);
    }
}
