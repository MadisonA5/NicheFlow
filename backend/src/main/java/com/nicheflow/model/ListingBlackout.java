/**
 * This program maps a supplier-declared blackout to the
 * listing_blackouts table for BO-01. It stores the listing and the
 * UTC start and end the supplier marked unavailable, separate from
 * the bookings lifecycle, plus an optional reason the supplier can
 * leave for themselves.
 *
 * File:   ListingBlackout.java
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
@Table(name = "listing_blackouts")
public class ListingBlackout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * ListingBlackout - This function gives JPA a no-argument
     * constructor so it can load rows into empty instances.
     */
    protected ListingBlackout() {
        // To leave fields unset until JPA copies the row onto this
        // instance
    }

    /**
     * ListingBlackout - This function builds a new blackout for a
     * supplier's listing. createdAt is stamped at construction time.
     *
     * Parameters:
     * listing - The listing being blacked out
     * startTime - The blackout start, stored as a UTC instant
     * endTime - The blackout end, stored as a UTC instant
     * reason - The supplier's optional note, or null
     */
    public ListingBlackout(Listing listing, Instant startTime,
            Instant endTime, String reason) {
        // To store the listing and the UTC window
        this.listing = listing;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;

        // To record when this blackout was added
        this.createdAt = Instant.now();
    }

    /**
     * getId - This function returns the database-generated identifier.
     *
     * Return - The blackout id, or null before the row is persisted
     */
    public Long getId() {
        // To return the primary key assigned when the row was saved
        return id;
    }

    /**
     * getListing - This function returns the listing this blackout
     * applies to.
     *
     * Return - The Listing being blacked out
     */
    public Listing getListing() {
        // To return the listing this blackout points at
        return listing;
    }

    /**
     * setListing - This function replaces the blacked-out listing.
     *
     * Parameters:
     * listing - The listing this blackout should point at
     */
    public void setListing(Listing listing) {
        // To replace the stored listing after an update
        this.listing = listing;
    }

    /**
     * getStartTime - This function returns the blackout start.
     *
     * Return - The start instant in UTC
     */
    public Instant getStartTime() {
        // To return the UTC instant the blackout starts
        return startTime;
    }

    /**
     * setStartTime - This function replaces the blackout start.
     *
     * Parameters:
     * startTime - The new start instant in UTC
     */
    public void setStartTime(Instant startTime) {
        // To store a new UTC start on this blackout
        this.startTime = startTime;
    }

    /**
     * getEndTime - This function returns the blackout end.
     *
     * Return - The end instant in UTC
     */
    public Instant getEndTime() {
        // To return the UTC instant the blackout ends
        return endTime;
    }

    /**
     * setEndTime - This function replaces the blackout end.
     *
     * Parameters:
     * endTime - The new end instant in UTC
     */
    public void setEndTime(Instant endTime) {
        // To store a new UTC end on this blackout
        this.endTime = endTime;
    }

    /**
     * getReason - This function returns the supplier's note for this
     * blackout.
     *
     * Return - The reason, or null when the supplier left it blank
     */
    public String getReason() {
        // To return the note the supplier left for this blackout
        return reason;
    }

    /**
     * setReason - This function replaces the supplier's note.
     *
     * Parameters:
     * reason - The new note to store, or null
     */
    public void setReason(String reason) {
        // To replace the stored note on this blackout
        this.reason = reason;
    }

    /**
     * getCreatedAt - This function returns when this blackout was
     * added.
     *
     * Return - The creation instant in UTC
     */
    public Instant getCreatedAt() {
        // To return the UTC instant this blackout was stored
        return createdAt;
    }
}
