/**
 * This program is the boundary-value suite for BookingPriceCalculator.
 * It checks the day count at the one-day boundary (exactly 24 hours
 * and one second more), short slots that still cost one day, and
 * whole multi-day bookings like the ones the frontend creates.
 *
 * File:   BookingPriceCalculatorTest.java
 * Course: AUCSC 325
 */
package com.nicheflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BookingPriceCalculatorTest {

    private static final Instant START = Instant.parse(
            "2027-01-10T00:00:00Z");

    /**
     * dayCountRoundsPartDaysUpAndIsAtLeastOne - This function checks
     * the number of days charged for bookings of different lengths.
     *
     * Parameters:
     * seconds - How long the booking lasts
     * expectedDays - The days that should be charged
     */
    @ParameterizedTest(name = "{0} seconds is {1} day(s)")
    @CsvSource({
        "1, 1",
        "3600, 1",
        "86400, 1",
        "86401, 2",
        "172800, 2",
        "432000, 5",
    })
    void dayCountRoundsPartDaysUpAndIsAtLeastOne(long seconds,
            long expectedDays) {
        assertThat(BookingPriceCalculator.days(START,
                START.plusSeconds(seconds))).isEqualTo(expectedDays);
    }

    /**
     * fiveDayBookingCostsFiveDays - This function prices the booking
     * from issue #10: five days at 50.00 per day.
     */
    @Test
    void fiveDayBookingCostsFiveDays() {
        assertThat(BookingPriceCalculator.totalPrice(new BigDecimal("50.00"),
                START, Instant.parse("2027-01-15T00:00:00Z")))
                .isEqualByComparingTo("250.00");
    }

    /**
     * oneHourSlotCostsOneDay - This function keeps the existing
     * behaviour for short slots: they cost the listing price.
     */
    @Test
    void oneHourSlotCostsOneDay() {
        assertThat(BookingPriceCalculator.totalPrice(new BigDecimal("25.00"),
                START, START.plusSeconds(3600)))
                .isEqualByComparingTo("25.00");
    }

    /**
     * totalHasScaleTwo - This function checks the total is money with
     * two decimal places, like the refund.
     */
    @Test
    void totalHasScaleTwo() {
        assertThat(BookingPriceCalculator.totalPrice(new BigDecimal("19.99"),
                START, START.plusSeconds(3 * 86400)))
                .isEqualTo(new BigDecimal("59.97"));
    }
}
