/**
 * This program is the Spring Data access point for the listings table
 * so LST-01 can persist and look up listings without writing SQL.
 *
 * File:   ListingRepository.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nicheflow.model.Listing;

public interface ListingRepository extends JpaRepository<Listing, Long> {
}
