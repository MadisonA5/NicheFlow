/**
 * This program is the Spring Data access point for the bookings
 * table so BK-01 can persist a PENDING booking. Overlap rejection
 * is the database exclusion constraint, not a query in this file.
 *
 * File:   BookingRepository.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nicheflow.model.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Loads a customer's booking history with listing details available
     * while the response DTO is mapped.
     */
    @Query("""
            SELECT booking FROM Booking booking
            JOIN FETCH booking.listing listing
            JOIN FETCH listing.supplier
            JOIN FETCH booking.customer
            WHERE booking.customer.id = :customerId
            ORDER BY booking.startTime DESC
            """)
    List<Booking> findAllForCustomer(
            @Param("customerId") Long customerId);

    /**
     * Loads booking requests for listings owned by the supplier.
     */
    @Query("""
            SELECT booking FROM Booking booking
            JOIN FETCH booking.listing listing
            JOIN FETCH listing.supplier
            JOIN FETCH booking.customer
            WHERE listing.supplier.id = :supplierId
            ORDER BY booking.startTime DESC
            """)
    List<Booking> findAllForSupplier(
            @Param("supplierId") Long supplierId);

    /**
     * Loads active booking windows for the public availability view.
     * Cancelled bookings do not reserve dates.
     */
    @Query("""
            SELECT booking FROM Booking booking
            WHERE booking.listing.id = :listingId
              AND booking.status IN (
                  com.nicheflow.model.BookingStatus.PENDING,
                  com.nicheflow.model.BookingStatus.ACCEPTED,
                  com.nicheflow.model.BookingStatus.CONFIRMED)
            ORDER BY booking.startTime
            """)
    List<Booking> findUnavailableBookingsByListingId(
            @Param("listingId") Long listingId);

    /**
     * findByListingId - This function returns every booking stored
     * for one listing so a caller can see how many attempts
     * actually persisted.
     *
     * Parameters:
     * listingId - The listing whose bookings to load
     *
     * Return - The bookings for that listing, possibly empty
     */
    @Query("""
            SELECT booking FROM Booking booking
            WHERE booking.listing.id = :listingId
            """)
    List<Booking> findByListingId(@Param("listingId") Long listingId);
}
