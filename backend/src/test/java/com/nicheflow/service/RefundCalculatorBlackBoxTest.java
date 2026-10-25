/**
 * This program is the black-box test suite for BK-04 refunds, written
 * from the RefundCalculator specification: 24 hours or more before the
 * start is a full refund, later but still before the start is half
 * (rounded half up to 2 decimals), at or after the start is zero. It
 * is a plain unit test: no Spring and no Docker.
 *
 * File:   RefundCalculatorBlackBoxTest.java
 * Course: AUCSC 325
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RefundCalculatorBlackBoxTest {

    private static final Instant START =
            Instant.parse("2026-11-02T15:00:00Z");
    private static final Instant CUTOFF =
            START.minus(24, ChronoUnit.HOURS);

    /** Refund for a price, using the shared start time. */
    private static BigDecimal refund(String price, Instant cancelledAt) {
        return RefundCalculator.calculateRefund(new BigDecimal(price),
                START, cancelledAt);
    }

    /**
     * Compares value AND scale: "5.00" does not equal "5.0", so this
     * also checks every refund comes back with two decimals.
     */
    private static void assertRefund(String expected, BigDecimal actual) {
        assertEquals(new BigDecimal(expected), actual);
    }

    // =================================================================
    // BOUNDARY VALUES: the two time cut-offs, one nanosecond each side
    // =================================================================

    /** Exactly 24 hours before the start is still a full refund. */
    @Test
    void exactlyTwentyFourHoursBeforeIsFull() {
        assertRefund("10.00", refund("10.00", CUTOFF));
    }

    /** One nanosecond earlier than the cut-off is a full refund. */
    @Test
    void oneNanosecondBeforeTheCutoffIsFull() {
        assertRefund("10.00", refund("10.00", CUTOFF.minusNanos(1)));
    }

    /** One nanosecond after the cut-off drops to half. */
    @Test
    void oneNanosecondAfterTheCutoffIsHalf() {
        assertRefund("5.00", refund("10.00", CUTOFF.plusNanos(1)));
    }

    /** One nanosecond before the start is still half. */
    @Test
    void oneNanosecondBeforeTheStartIsHalf() {
        assertRefund("5.00", refund("10.00", START.minusNanos(1)));
    }

    /** At the exact start the refund is zero. */
    @Test
    void exactlyAtTheStartIsZero() {
        assertRefund("0.00", refund("10.00", START));
    }

    /** One nanosecond after the start the refund is zero. */
    @Test
    void oneNanosecondAfterTheStartIsZero() {
        assertRefund("0.00", refund("10.00", START.plusNanos(1)));
    }

    // =================================================================
    // EQUIVALENCE CLASSES: one typical time from each of the 3 zones
    // =================================================================

    /** Zone 1, well before the cut-off (7 days early): full price. */
    @Test
    void sevenDaysBeforeIsFull() {
        assertRefund("10.00",
                refund("10.00", START.minus(7, ChronoUnit.DAYS)));
    }

    /** Zone 2, inside the last 24 hours (12 hours early): half. */
    @Test
    void twelveHoursBeforeIsHalf() {
        assertRefund("5.00",
                refund("10.00", START.minus(12, ChronoUnit.HOURS)));
    }

    /** Zone 3, after the booking has started (a day late): zero. */
    @Test
    void oneDayAfterTheStartIsZero() {
        assertRefund("0.00",
                refund("10.00", START.plus(1, ChronoUnit.DAYS)));
    }

    // =================================================================
    // PRICE VALUES: rounding (half up) and scale of the half refund
    // =================================================================

    /** Half refunds: price -> expected, including rounding cases. */
    @ParameterizedTest(name = "half of {0} = {1}")
    @CsvSource({
            "10.00,5.00",
            "10,5.00",                 // price given with no decimals
            "10.01,5.01",              // 5.005 rounds up
            "0.01,0.01",               // 0.005 rounds up
            "0.02,0.01",               // exact half
            "0.03,0.02",               // 0.015 rounds up
            "0.00,0.00",               // free listing
            "99999999.99,50000000.00"  // largest price, rounds up
    })
    void halfRefundRoundsHalfUp(String price, String expected) {
        assertRefund(expected, refund(price, START.minusNanos(1)));
    }

    /** Full refunds: the price comes back unchanged, at scale 2. */
    @ParameterizedTest(name = "full refund of {0} = {1}")
    @CsvSource({
            "10.00,10.00",
            "10,10.00",                // scale is fixed to 2
            "0.01,0.01",
            "0.00,0.00",
            "99999999.99,99999999.99"
    })
    void fullRefundKeepsThePrice(String price, String expected) {
        assertRefund(expected, refund(price, CUTOFF));
    }

    /** Zero refunds come back as 0.00 whatever the price. */
    @ParameterizedTest(name = "price {0} cancelled at start -> 0.00")
    @CsvSource({"10.00", "0.01", "99999999.99"})
    void zeroRefundIsAlwaysZeroPointZeroZero(String price) {
        assertRefund("0.00", refund(price, START));
    }
}
