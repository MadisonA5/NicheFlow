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
