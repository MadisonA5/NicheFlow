/**
 * This program proves the BK-05 storage choice. Booking times are
 * Instants, so two zones that display different wall clocks still
 * name the same moment, and a window that crosses a daylight-saving
 * change keeps its real UTC length.
 *
 * File:   BookingTimezoneTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookingTimezoneTest {

    private static final ZoneId EDMONTON = ZoneId.of(
            "America/Edmonton");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    /**
     * The stored moment both viewers read. Edmonton shows
     * 2026-07-15 12:00 (UTC-6). Tokyo shows 2026-07-16 03:00
     * (UTC+9).
     */
    private static final Instant STORED = Instant.parse(
            "2026-07-15T18:00:00Z");

    /**
     * crossTimezoneAgreementOnTheSameInstant - This function reads
     * one stored instant in America/Edmonton and in Asia/Tokyo.
     * Edmonton wall clock is 2026-07-15 12:00. Tokyo wall clock is
     * 2026-07-16 03:00. Both convert back to the same Instant.
     */
    @Test
    void crossTimezoneAgreementOnTheSameInstant() {
        // To show the window in each viewer's zone
        ZonedDateTime supplier = STORED.atZone(EDMONTON);
        ZonedDateTime customer = STORED.atZone(TOKYO);

        // To prove the local hour and the local date both differ
        assertThat(supplier.getHour()).isNotEqualTo(customer.getHour());
        assertThat(supplier.toLocalDate())
                .isNotEqualTo(customer.toLocalDate());

        // To prove both displays are the stored instant
        assertThat(supplier.toInstant()).isEqualTo(STORED);
        assertThat(customer.toInstant()).isEqualTo(STORED);
    }

    /**
     * springForwardWindowKeepsTheCorrectRealDuration - This function
     * books America/Edmonton local 2026-03-08 01:30 through 03:30.
     * Clocks skip 02:00 that morning, so subtracting the wall-clock
     * hours gives 2 hours while the real UTC length is 1 hour.
     */
    @Test
    void springForwardWindowKeepsTheCorrectRealDuration() {
        // To cross the gap from 02:00 to 03:00
        ZonedDateTime startLocal = ZonedDateTime.of(
                2026, 3, 8, 1, 30, 0, 0, EDMONTON);
        ZonedDateTime endLocal = ZonedDateTime.of(
                2026, 3, 8, 3, 30, 0, 0, EDMONTON);

        assertThat(wallClockDuration(startLocal, endLocal))
                .isEqualTo(Duration.ofHours(2));
        assertThat(realDuration(startLocal, endLocal))
                .isEqualTo(Duration.ofHours(1));
    }

    /**
     * fallBackWindowKeepsTheCorrectRealDuration - This function
     * books America/Edmonton local 2026-11-01 00:30 through 02:30.
     * The 01:00 hour happens twice that morning, so subtracting the
     * wall-clock hours gives 2 hours while the real UTC length is
     * 3 hours. 00:30 is before the overlap and 02:30 is after it.
     */
    @Test
    void fallBackWindowKeepsTheCorrectRealDuration() {
        // To cross the repeated hour from 01:00 to 02:00
        ZonedDateTime startLocal = ZonedDateTime.of(
                2026, 11, 1, 0, 30, 0, 0, EDMONTON);
        ZonedDateTime endLocal = ZonedDateTime.of(
                2026, 11, 1, 2, 30, 0, 0, EDMONTON);

        assertThat(wallClockDuration(startLocal, endLocal))
                .isEqualTo(Duration.ofHours(2));
        assertThat(realDuration(startLocal, endLocal))
                .isEqualTo(Duration.ofHours(3));
    }

    /**
     * instantStorageIsZoneIndependent - This function builds one
     * moment two ways: America/Edmonton 2026-07-15 12:00, and
     * Asia/Tokyo 2026-07-16 03:00. Both Instants match, and both
     * format as the ISO-8601 UTC string BookingResponse returns
     * for an Instant field, 2026-07-15T18:00:00Z.
     */
    @Test
    void instantStorageIsZoneIndependent() {
        // To build the same moment from each viewer's wall clock
        Instant fromEdmonton = ZonedDateTime.of(
                2026, 7, 15, 12, 0, 0, 0, EDMONTON).toInstant();
        Instant fromTokyo = ZonedDateTime.of(
                2026, 7, 16, 3, 0, 0, 0, TOKYO).toInstant();

        assertThat(fromEdmonton).isEqualTo(fromTokyo);
        assertThat(utcString(fromEdmonton))
                .isEqualTo("2026-07-15T18:00:00Z");
        assertThat(utcString(fromTokyo))
                .isEqualTo("2026-07-15T18:00:00Z");
    }

    /**
     * wallClockDuration - This function subtracts the local times
     * the way a wall clock would, ignoring the zone offset.
     *
     * Parameters:
     * startLocal - The zoned start of the window
     * endLocal - The zoned end of the window
     *
     * Return - The naive local-time length
     */
    private static Duration wallClockDuration(ZonedDateTime startLocal,
            ZonedDateTime endLocal) {
        return Duration.between(startLocal.toLocalTime(),
                endLocal.toLocalTime());
    }

    /**
     * realDuration - This function measures the window after both
     * ends have been converted to Instants, which is how a booking
     * stores them.
     *
     * Parameters:
     * startLocal - The zoned start of the window
     * endLocal - The zoned end of the window
     *
     * Return - The elapsed UTC length
     */
    private static Duration realDuration(ZonedDateTime startLocal,
            ZonedDateTime endLocal) {
        return Duration.between(startLocal.toInstant(),
                endLocal.toInstant());
    }

    /**
     * utcString - This function writes an Instant in the ISO-8601
     * UTC form Jackson uses for BookingResponse startTime and
     * endTime.
     *
     * Parameters:
     * moment - The stored instant to format
     *
     * Return - The UTC timestamp, ending in Z
     */
    private static String utcString(Instant moment) {
        return DateTimeFormatter.ISO_INSTANT.format(moment);
    }
}
