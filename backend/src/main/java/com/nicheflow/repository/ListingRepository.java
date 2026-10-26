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
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nicheflow.model.Listing;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    /**
     * Loads every listing owned by the specified supplier, including
     * inactive listings for the supplier account page.
     */
    @Query("""
            SELECT listing FROM Listing listing
            JOIN FETCH listing.supplier
            WHERE listing.supplier.id = :supplierId
            ORDER BY listing.id DESC
            """)
    List<Listing> findAllBySupplierId(@Param("supplierId") Long supplierId);

    /**
     * searchActive - This function returns one page of active
     * listings whose title contains the keyword (case-insensitive)
     * and whose category equals the given category. Optional min
     * and max prices bound the range inclusively. A null filter is
     * skipped. Supplier is fetched so ListingResponse can read the
     * owner after the transaction ends.
     *
     * Parameters:
     * keyword - The already-escaped title fragment, or an empty string
     * category - The exact category, or an empty string
     * hasMinPrice - Whether to apply the inclusive lower price
     * minPrice - The lower price, or zero when its filter is omitted
     * hasMaxPrice - Whether to apply the inclusive upper price
     * maxPrice - The upper price, or zero when its filter is omitted
     * pageable - The page number, size, and sort
     *
     * Return - One page of matching active listings
     */
    @Query(value = """
            SELECT listing FROM Listing listing
            JOIN FETCH listing.supplier
            WHERE listing.active = true
              AND (:keyword = ''
                   OR LOWER(listing.title) LIKE LOWER(
                           CONCAT('%', :keyword, '%')) ESCAPE '\\')
              AND (:category = ''
                   OR listing.category = :category)
              AND (:hasMinPrice = false
                   OR listing.price >= :minPrice)
              AND (:hasMaxPrice = false
                   OR listing.price <= :maxPrice)
            """,
            countQuery = """
            SELECT COUNT(listing) FROM Listing listing
            WHERE listing.active = true
              AND (:keyword = ''
                   OR LOWER(listing.title) LIKE LOWER(
                           CONCAT('%', :keyword, '%')) ESCAPE '\\')
              AND (:category = ''
                   OR listing.category = :category)
              AND (:hasMinPrice = false
                   OR listing.price >= :minPrice)
              AND (:hasMaxPrice = false
                   OR listing.price <= :maxPrice)
            """)
    Page<Listing> searchActive(@Param("keyword") String keyword,
            @Param("category") String category,
            @Param("hasMinPrice") boolean hasMinPrice,
            @Param("minPrice") BigDecimal minPrice,
            @Param("hasMaxPrice") boolean hasMaxPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
