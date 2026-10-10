/**
 * This program builds listings with an id, which is normally assigned
 * by the database, so LST-01 unit tests can check ownership.
 *
 * File:   TestListings.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow;

import java.math.BigDecimal;

import org.springframework.test.util.ReflectionTestUtils;

import com.nicheflow.model.Listing;
import com.nicheflow.model.User;

public final class TestListings {

    /**
     * TestListings - This function hides the helper constructor so
     * tests only call withId.
     */
    private TestListings() {
    }

    /**
     * withId - This function builds a listing row the tests can treat
     * as already persisted.
     *
     * Parameters:
     * id - The listing id to stamp on the entity
     * title - The display name
     * category - The category string
     * price - The price with two decimal places
     * supplier - The owning User, already given an id
     *
     * Return - The listing with that id
     */
    public static Listing withId(long id, String title, String category,
            BigDecimal price, User supplier) {
        Listing listing = new Listing(title, category, price, supplier);
        ReflectionTestUtils.setField(listing, "id", id);
        return listing;
    }
}
