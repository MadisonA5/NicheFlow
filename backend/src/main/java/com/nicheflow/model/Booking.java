/**
 * This program maps a customer booking to the bookings table for
 * BK-02. It stores the listing, the customer, the lifecycle status,
 * and the UTC start and end so the booking can move from pending
 * through confirmed and completed, or to cancelled. refund_amount
 * stays null until a cancellation stores it.
 *
 * File:   Booking.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.model;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount;

    /**
     * Booking - This function gives JPA a no-argument constructor so
     * it can load rows into empty instances.
     */
    protected Booking() {
        // To leave fields unset until JPA copies the row onto this
        // instance
    }

    /**
     * Booking - This function builds a new booking for a customer on
     * a listing. Status starts at PENDING, the first state in the
     * BK-02 lifecycle.
     *
     * Parameters:
     * listing - The listing the customer is booking
     * customer - The User who is making the booking
     * startTime - The slot start, stored as a UTC instant
     * endTime - The slot end, stored as a UTC instant
     */
    public Booking(Listing listing, User customer, Instant startTime,
            Instant endTime) {
        // To store the listing, customer, and UTC slot
        this.listing = listing;
        this.customer = customer;
        this.startTime = startTime;
        this.endTime = endTime;

        // To start the BK-02 lifecycle at pending
        this.status = BookingStatus.PENDING;
    }

    /**
     * getId - This function returns the database-generated identifier.
     *
     * Return - The booking id, or null before the row is persisted
     */
    public Long getId() {
        // To return the primary key assigned when the row was saved
        return id;
    }

    /**
     * getListing - This function returns the listing this booking is
     * for.
     *
     * Return - The Listing being booked
     */
    public Listing getListing() {
        // To return the listing this booking points at
        return listing;
    }

    /**
     * setListing - This function replaces the booked listing.
     *
     * Parameters:
     * listing - The listing this booking should point at
     */
    public void setListing(Listing listing) {
        // To replace the stored listing after an update
        this.listing = listing;
    }

    /**
     * getCustomer - This function returns the customer who made the
     * booking.
     *
     * Return - The User who booked the listing
     */
    public User getCustomer() {
        // To return the customer account stored on this booking
        return customer;
    }

    /**
     * setCustomer - This function replaces the customer on this
     * booking.
     *
     * Parameters:
     * customer - The User who should own this booking
     */
    public void setCustomer(User customer) {
        // To replace the stored customer after an update
        this.customer = customer;
    }

    /**
     * getStatus - This function returns the BK-02 lifecycle state.
     *
     * Return - PENDING, ACCEPTED, CONFIRMED, COMPLETED, or CANCELLED
     */
    public BookingStatus getStatus() {
        // To return the lifecycle state stored on this booking
        return status;
    }

    /**
     * setStatus - This function replaces the stored lifecycle state.
     *
     * Parameters:
     * status - PENDING, ACCEPTED, CONFIRMED, COMPLETED, or CANCELLED
     */
    public void setStatus(BookingStatus status) {
        // To store the lifecycle state on this booking
        this.status = status;
    }

    /**
     * getStartTime - This function returns the slot start.
     *
     * Return - The start instant in UTC
     */
    public Instant getStartTime() {
        // To return the UTC instant the booking starts
        return startTime;
    }

    /**
     * setStartTime - This function replaces the slot start.
     *
     * Parameters:
     * startTime - The new start instant in UTC
     */
    public void setStartTime(Instant startTime) {
        // To store a new UTC start on this booking
        this.startTime = startTime;
    }

    /**
     * getEndTime - This function returns the slot end.
     *
     * Return - The end instant in UTC
     */
    public Instant getEndTime() {
        // To return the UTC instant the booking ends
        return endTime;
    }

    /**
     * setEndTime - This function replaces the slot end.
     *
     * Parameters:
     * endTime - The new end instant in UTC
     */
    public void setEndTime(Instant endTime) {
        // To store a new UTC end on this booking
        this.endTime = endTime;
    }

    /**
     * getRefundAmount - This function returns the refund stored when
     * the booking was cancelled.
     *
     * Return - The refund, or null when the booking is not cancelled
     */
    public BigDecimal getRefundAmount() {
        // To return the amount written by cancel
        return refundAmount;
    }

    /**
     * setRefundAmount - This function stores the refund for a
     * cancellation.
     *
     * Parameters:
     * refundAmount - The refund to store, or null
     */
    public void setRefundAmount(BigDecimal refundAmount) {
        // To store the refund on this booking
        this.refundAmount = refundAmount;
    }
}
