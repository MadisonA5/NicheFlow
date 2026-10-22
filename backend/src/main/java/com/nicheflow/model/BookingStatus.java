/**
 * This program defines the booking lifecycle states for BK-02:
 * pending, confirmed, completed, and cancelled. The value is stored
 * as the enum name so each state stays readable in the database.
 *
 * File:   BookingStatus.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.model;

public enum BookingStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,
    CANCELLED
}
