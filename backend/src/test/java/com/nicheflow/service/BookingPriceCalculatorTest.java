/**
 * This program proves the issue #14 booking total. The listing price
 * is charged once per started hour for an HOUR listing and once per
 * started day for a DAY listing, with boundary values on either side
 * of a whole unit.
 *
 * File:   BookingPriceCalculatorTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import com.nicheflow.model.PriceUnit;

class BookingPriceCalculatorTest {

    private static final Instant T10 = Instant.parse(
            "2031-03-01T10:00:00Z");
    private static final BigDecimal PRICE = new BigDecimal("12.50");

    /** Two whole hours on an HOUR listing are two units. */
    @Test
    void twoWholeHoursAreTwoUnits() {
        assertThat(BookingPriceCalculator.units(PriceUnit.HOUR, T10,
                T10.plusSeconds(2 * 3600))).isEqualTo(2);
    }

    /** Boundary: one second past two hours starts a third hour. */
    @Test
    void oneSecondPastAWholeHourStartsTheNextHour() {
        assertThat(BookingPriceCalculator.units(PriceUnit.HOUR, T10,
                T10.plusSeconds(2 * 3600 + 1))).isEqualTo(3);
    }

    /** Boundary: the smallest window, one second, is one hour. */
    @Test
    void oneSecondIsOneHour() {
        assertThat(BookingPriceCalculator.units(PriceUnit.HOUR, T10,
                T10.plusSeconds(1))).isEqualTo(1);
    }

    /** An overnight stay, 15:00 to 11:00 the next day, is one day. */
    @Test
    void overnightCheckInToCheckOutIsOneDay() {
        Instant checkIn = Instant.parse("2026-11-13T22:00:00Z");
        Instant checkOut = Instant.parse("2026-11-14T18:00:00Z");

        assertThat(BookingPriceCalculator.units(PriceUnit.DAY, checkIn,
                checkOut)).isEqualTo(1);
    }

    /** Boundary: exactly 24 hours is one day, one second more is two. */
    @Test
    void oneSecondPastADayIsTwoDays() {
        Instant end = T10.plusSeconds(24 * 3600);

        assertThat(BookingPriceCalculator.units(PriceUnit.DAY, T10, end))
                .isEqualTo(1);
        assertThat(BookingPriceCalculator.units(PriceUnit.DAY, T10,
                end.plusSeconds(1))).isEqualTo(2);
    }

    /**
     * An hourly window across the Edmonton fall-back hour is priced by
     * real elapsed time: 00:30 to 02:30 local is three hours.
     */
    @Test
    void hourlyWindowAcrossFallBackIsPricedByRealTime() {
        ZoneId edmonton = ZoneId.of("America/Edmonton");
        Instant start = ZonedDateTime.of(2026, 11, 1, 0, 30, 0, 0,
                edmonton).toInstant();
        Instant end = ZonedDateTime.of(2026, 11, 1, 2, 30, 0, 0,
                edmonton).toInstant();

        assertThat(BookingPriceCalculator.units(PriceUnit.HOUR, start,
                end)).isEqualTo(3);
    }

    /** The total is the price times the units, at scale 2. */
    @Test
    void totalMultipliesThePriceByTheUnits() {
        assertThat(BookingPriceCalculator.total(PRICE, PriceUnit.HOUR,
                T10, T10.plusSeconds(3 * 3600)))
                .isEqualTo(new BigDecimal("37.50"));
    }
}
