/**
 * This program builds blackouts with an id, which is normally
 * assigned by the database, so BO-01 unit tests can check ownership
 * and overlap handling.
 *
 * File:   TestBlackouts.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow;

import java.time.Instant;

import org.springframework.test.util.ReflectionTestUtils;

import com.nicheflow.model.Listing;
import com.nicheflow.model.ListingBlackout;

public final class TestBlackouts {

    /**
     * TestBlackouts - This function hides the helper constructor so
     * tests only call withId.
     */
    private TestBlackouts() {
    }

    /**
     * withId - This function builds a blackout the tests can treat
     * as already persisted.
     *
     * Parameters:
     * id - The blackout id to stamp on the entity
     * listing - The listing being blacked out, already given an id
     * startTime - The blackout start, a UTC instant
     * endTime - The blackout end, a UTC instant
     * reason - The supplier's note, or null
     *
     * Return - The blackout with that id
     */
    public static ListingBlackout withId(long id, Listing listing,
            Instant startTime, Instant endTime, String reason) {
        ListingBlackout blackout = new ListingBlackout(listing, startTime,
                endTime, reason);
        ReflectionTestUtils.setField(blackout, "id", id);
        return blackout;
    }
}
