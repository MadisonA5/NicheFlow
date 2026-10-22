/**
 * This program builds bookings with an id, which is normally assigned
 * by the database, so BK-01 unit tests can check the create payload.
 *
 * File:   TestBookings.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.User;

public final class TestBookings {

    /**
     * TestBookings - This function hides the helper constructor so
     * tests only call withId.
     */
    private TestBookings() {
    }

    /**
     * withId - This function builds a pending booking the tests can
     * treat as already persisted.
     *
     * Parameters:
     * id - The booking id to stamp on the entity
     * listing - The listing being booked, already given an id
     * customer - The customer, already given an id
     * startTime - The slot start, a UTC instant
     * endTime - The slot end, a UTC instant
     *
     * Return - The pending booking with that id
     */
    public static Booking withId(long id, Listing listing, User customer,
            Instant startTime, Instant endTime) {
        Booking booking = new Booking(listing, customer, startTime,
                endTime);
        ReflectionTestUtils.setField(booking, "id", id);
        return booking;
    }
}
