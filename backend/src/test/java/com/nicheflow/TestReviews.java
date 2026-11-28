/**
 * This program builds reviews with an id, which is normally assigned
 * by the database, so ADM-02 unit tests can check reviewer, reviewee,
 * and rating handling.
 *
 * File:   TestReviews.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow;

import org.springframework.test.util.ReflectionTestUtils;

import com.nicheflow.model.Booking;
import com.nicheflow.model.Review;
import com.nicheflow.model.User;

public final class TestReviews {

    /**
     * TestReviews - This function hides the helper constructor so
     * tests only call withId.
     */
    private TestReviews() {
    }

    /**
     * withId - This function builds a review the tests can treat as
     * already persisted.
     *
     * Parameters:
     * id - The review id to stamp on the entity
     * booking - The booking being reviewed, already given an id
     * reviewer - The party leaving this review, already given an id
     * reviewee - The party this review is about, already given an id
     * rating - The 1-5 star rating
     * comment - The reviewer's note, or null
     *
     * Return - The review with that id
     */
    public static Review withId(long id, Booking booking, User reviewer,
            User reviewee, int rating, String comment) {
        Review review = new Review(booking, reviewer, reviewee, rating,
                comment);
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }
}
