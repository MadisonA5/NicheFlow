/**
 * This program is the Spring Data access point for the
 * listing_blackouts table so BO-01 can store, list, and check
 * overlap for supplier blackouts.
 *
 * File:   ListingBlackoutRepository.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nicheflow.model.ListingBlackout;

public interface ListingBlackoutRepository
        extends JpaRepository<ListingBlackout, Long> {

    /**
     * findByListingId - This function returns every blackout stored
     * for one listing, earliest first. Used both to merge into the
     * public availability view and to list a supplier's own
     * blackouts for management.
     *
     * Parameters:
     * listingId - The listing whose blackouts to load
     *
     * Return - The blackouts for that listing, possibly empty
     */
    @Query("""
            SELECT blackout FROM ListingBlackout blackout
            WHERE blackout.listing.id = :listingId
            ORDER BY blackout.startTime
            """)
    List<ListingBlackout> findByListingId(
            @Param("listingId") Long listingId);

    /**
     * existsOverlapping - This function reports whether any blackout
     * stored for a listing overlaps the given window. Used to reject
     * a booking that falls inside a blackout, and to reject a new
     * blackout that overlaps one already stored.
     *
     * Parameters:
     * listingId - The listing to check
     * startTime - The window start, a UTC instant
     * endTime - The window end, a UTC instant
     *
     * Return - True when a stored blackout overlaps the window
     */
    @Query("""
            SELECT COUNT(blackout) > 0 FROM ListingBlackout blackout
            WHERE blackout.listing.id = :listingId
              AND blackout.startTime < :endTime
              AND blackout.endTime > :startTime
            """)
    boolean existsOverlapping(@Param("listingId") Long listingId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);
}
