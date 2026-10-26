/**
 * This program defines the booking lifecycle states for BK-02:
 * pending, accepted, confirmed, completed, and cancelled. A supplier
 * accepts a request before the customer confirms payment.
 *
 * File:   BookingStatus.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.model;

public enum BookingStatus {
    PENDING,
    ACCEPTED,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
