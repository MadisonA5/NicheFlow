/**
 * Boundary tests for the hourly BookingPriceCalculator.
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

    @ParameterizedTest(name = "{0} seconds is {1} hour(s)")
    @CsvSource({
        "1, 1",
        "3600, 1",
        "3601, 2",
        "7200, 2",
        "10800, 3",
    })
    void hourCountRoundsPartHoursUpAndIsAtLeastOne(long seconds,
            long expectedHours) {
        assertThat(BookingPriceCalculator.hours(START,
                START.plusSeconds(seconds))).isEqualTo(expectedHours);
    }

    @Test
    void threeHourBookingCostsThreeHours() {
        assertThat(BookingPriceCalculator.totalPrice(new BigDecimal("50.00"),
                START, START.plusSeconds(3 * 3600)))
                .isEqualByComparingTo("150.00");
    }

    @Test
    void oneHourBookingCostsOneHour() {
        assertThat(BookingPriceCalculator.totalPrice(new BigDecimal("25.00"),
                START, START.plusSeconds(3600)))
                .isEqualByComparingTo("25.00");
    }

    @Test
    void totalHasScaleTwo() {
        assertThat(BookingPriceCalculator.totalPrice(new BigDecimal("19.99"),
                START, START.plusSeconds(3 * 3600)))
                .isEqualTo(new BigDecimal("59.97"));
    }
}
