/**
 * This program defines what one listing price buys for issue #14:
 * one day or one hour of the listing. A booking is charged the
 * price once for every started unit of its window.
 *
 * File:   PriceUnit.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.model;

import java.time.Duration;

public enum PriceUnit {
    DAY(Duration.ofDays(1)),
    HOUR(Duration.ofHours(1));

    private final Duration length;

    /**
     * PriceUnit - This function stores how long one priced unit is.
     *
     * Parameters:
     * length - The real elapsed time one unit covers
     */
    PriceUnit(Duration length) {
        this.length = length;
    }

    /**
     * length - This function returns how long one priced unit is.
     *
     * Return - One day or one hour
     */
    public Duration length() {
        return length;
    }
}
