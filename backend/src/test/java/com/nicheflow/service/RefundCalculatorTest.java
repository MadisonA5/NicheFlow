/**
 * This program proves the BK-04 refund boundaries. A cancellation
 * 24 hours or more before the start returns the full price. Inside
 * that window, still before the start, returns half. At or after
 * the start returns zero.
 *
 * File:   RefundCalculatorTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundCalculatorTest {

    private static final Instant START = Instant.parse(
            "2026-11-02T15:00:00Z");
    private static final BigDecimal PRICE = new BigDecimal("10.01");

    /**
     * moreThan24HoursBeforeStartReturnsTheFullPrice - This function
     * checks a cancellation one nanosecond earlier than the 24 hour
     * cutoff.
     */
    @Test
    void moreThan24HoursBeforeStartReturnsTheFullPrice() {
        Instant cancelledAt = START.minus(24, ChronoUnit.HOURS)
                .minusNanos(1);

        assertThat(RefundCalculator.calculateRefund(PRICE, START,
                cancelledAt)).isEqualByComparingTo("10.01");
    }

    /**
     * exactly24HoursBeforeStartReturnsTheFullPrice - This function
     * checks the cutoff itself. That instant is the full-price
     * side, not the half-price side.
     */
    @Test
    void exactly24HoursBeforeStartReturnsTheFullPrice() {
        Instant cancelledAt = START.minus(24, ChronoUnit.HOURS);

        assertThat(RefundCalculator.calculateRefund(PRICE, START,
                cancelledAt)).isEqualByComparingTo("10.01");
    }

    /**
     * insideTheLast24HoursReturnsHalfRoundedHalfUp - This function
     * checks a cancellation one nanosecond after the cutoff. 10.01
     * times one half rounds half up to 5.01.
     */
    @Test
    void insideTheLast24HoursReturnsHalfRoundedHalfUp() {
        Instant cancelledAt = START.minus(24, ChronoUnit.HOURS)
                .plusNanos(1);

        assertThat(RefundCalculator.calculateRefund(PRICE, START,
                cancelledAt)).isEqualByComparingTo("5.01");
    }

    /**
     * atStartTimeReturnsZero - This function checks a cancellation
     * at the same instant the booking starts.
     */
    @Test
    void atStartTimeReturnsZero() {
        assertThat(RefundCalculator.calculateRefund(PRICE, START, START))
                .isEqualByComparingTo("0.00");
    }

    /**
     * afterStartTimeReturnsZero - This function checks a cancellation
     * after the booking has started.
     */
    @Test
    void afterStartTimeReturnsZero() {
        Instant cancelledAt = START.plusSeconds(1);

        assertThat(RefundCalculator.calculateRefund(PRICE, START,
                cancelledAt)).isEqualByComparingTo("0.00");
    }
}
