/**
 * This program is the Spring Data access point for the listings table
 * so LST-01 can persist listings and LST-02 can search active ones
 * without writing SQL in the service.
 *
 * File:   ListingRepository.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.repository;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nicheflow.model.Listing;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    /**
     * searchActive - This function returns one page of active
     * listings whose title contains the keyword (case-insensitive)
     * and whose category equals the given category. Optional min
     * and max prices bound the range inclusively. A null filter is
     * skipped. Supplier is fetched so ListingResponse can read the
     * owner after the transaction ends.
     *
     * Parameters:
     * keyword - The already-escaped title fragment, or null
     * category - The exact category, or null
     * minPrice - The inclusive lower price, or null
     * maxPrice - The inclusive upper price, or null
     * pageable - The page number, size, and sort
     *
     * Return - One page of matching active listings
     */
    @Query(value = """
            SELECT listing FROM Listing listing
            JOIN FETCH listing.supplier
            WHERE listing.active = true
              AND (:keyword IS NULL
                   OR LOWER(listing.title) LIKE LOWER(
                           CONCAT('%', :keyword, '%')) ESCAPE '\\')
              AND (:category IS NULL
                   OR listing.category = :category)
              AND (:minPrice IS NULL
                   OR listing.price >= :minPrice)
              AND (:maxPrice IS NULL
                   OR listing.price <= :maxPrice)
            """,
            countQuery = """
            SELECT COUNT(listing) FROM Listing listing
            WHERE listing.active = true
              AND (:keyword IS NULL
                   OR LOWER(listing.title) LIKE LOWER(
                           CONCAT('%', :keyword, '%')) ESCAPE '\\')
              AND (:category IS NULL
                   OR listing.category = :category)
              AND (:minPrice IS NULL
                   OR listing.price >= :minPrice)
              AND (:maxPrice IS NULL
                   OR listing.price <= :maxPrice)
            """)
    Page<Listing> searchActive(@Param("keyword") String keyword,
            @Param("category") String category,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
