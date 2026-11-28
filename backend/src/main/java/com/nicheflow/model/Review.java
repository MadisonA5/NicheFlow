/**
 * This program maps a party's rating of the other party on one
 * completed booking to the reviews table for ADM-02. A booking can
 * carry up to two reviews, one from each party, each pointing at the
 * other as the reviewee.
 *
 * File:   Review.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Review - This function gives JPA a no-argument constructor so
     * it can load rows into empty instances.
     */
    protected Review() {
        // To leave fields unset until JPA copies the row onto this
        // instance
    }

    /**
     * Review - This function builds a new review one party leaves
     * for the other on a completed booking. createdAt is stamped at
     * construction time.
     *
     * Parameters:
     * booking - The completed booking this review is about
     * reviewer - The party leaving this review
     * reviewee - The party this review is about
     * rating - The 1-5 star rating
     * comment - The reviewer's optional note, or null
     */
    public Review(Booking booking, User reviewer, User reviewee,
            int rating, String comment) {
        // To store who reviewed whom, and about which booking
        this.booking = booking;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.rating = rating;
        this.comment = comment;

        // To record when this review was added
        this.createdAt = Instant.now();
    }

    /**
     * getId - This function returns the database-generated identifier.
     *
     * Return - The review id, or null before the row is persisted
     */
    public Long getId() {
        // To return the primary key assigned when the row was saved
        return id;
    }

    /**
     * getBooking - This function returns the booking this review is
     * about.
     *
     * Return - The reviewed Booking
     */
    public Booking getBooking() {
        // To return the booking this review points at
        return booking;
    }

    /**
     * getReviewer - This function returns the party who left this
     * review.
     *
     * Return - The reviewer User
     */
    public User getReviewer() {
        // To return who left this review
        return reviewer;
    }

    /**
     * getReviewee - This function returns the party this review is
     * about.
     *
     * Return - The reviewee User
     */
    public User getReviewee() {
        // To return who this review is about
        return reviewee;
    }

    /**
     * getRating - This function returns the star rating left.
     *
     * Return - The rating, from 1 to 5
     */
    public int getRating() {
        // To return the stored 1-5 star rating
        return rating;
    }

    /**
     * getComment - This function returns the reviewer's note.
     *
     * Return - The comment, or null when the reviewer left it blank
     */
    public String getComment() {
        // To return the note the reviewer left, if any
        return comment;
    }

    /**
     * getCreatedAt - This function returns when this review was
     * added.
     *
     * Return - The creation instant in UTC
     */
    public Instant getCreatedAt() {
        // To return the UTC instant this review was stored
        return createdAt;
    }
}
