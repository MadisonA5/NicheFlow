/**
 * This program maps a supplier listing to the listings table for
 * LST-01. Availability slots are not stored here yet; this row is
 * the listing itself so later CRUD and ownership checks have a
 * place to persist title, category, price, location,
 * description, and active.
 *
 * File:   Listing.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.model;

import java.math.BigDecimal;

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
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false)
    private User supplier;

    @Column(nullable = false)
    private boolean active = true;

    /**
     * Listing - This function gives JPA a no-argument constructor so
     * it can load rows into empty instances.
     */
    protected Listing() {
        // To leave fields unset until JPA copies the row onto this
        // instance
    }

    /**
     * Listing - This function builds a new listing owned by a
     * supplier. Active defaults to true so a freshly created listing
     * is visible until the owner deactivates it.
     *
     * Parameters:
     * title - The display name of the listing
     * category - The search/filter category stored as text
     * price - The price with two decimal places
     * location - Where the listing is offered
     * description - The supplier's description of the listing
     * supplier - The User who owns this listing
     */
    public Listing(String title, String category, BigDecimal price,
            String location, String description, User supplier) {
        // To store the fields the create-listing path passes in
        this.title = title;
        this.category = category;
        this.price = price;
        this.location = location;
        this.description = description;
        this.supplier = supplier;
        this.active = true;
    }

    /**
     * getId - This function returns the database-generated identifier.
     *
     * Return - The listing id, or null before the row is persisted
     */
    public Long getId() {
        return id;
    }

    /**
     * getTitle - This function returns the listing display name.
     *
     * Return - The title
     */
    public String getTitle() {
        return title;
    }

    /**
     * setTitle - This function replaces the listing display name.
     *
     * Parameters:
     * title - The new title to store
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * getCategory - This function returns the listing category used
     * for later search filters.
     *
     * Return - The category string
     */
    public String getCategory() {
        return category;
    }

    /**
     * setCategory - This function replaces the listing category.
     *
     * Parameters:
     * category - The new category to store
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * getPrice - This function returns the listing price.
     *
     * Return - The price with scale 2
     */
    public BigDecimal getPrice() {
        return price;
    }

    /**
     * setPrice - This function replaces the listing price.
     *
     * Parameters:
     * price - The new price to store
     */
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    /**
     * getLocation - This function returns where the listing is
     * offered.
     *
     * Return - The location
     */
    public String getLocation() {
        return location;
    }

    /**
     * setLocation - This function replaces where the listing is
     * offered.
     *
     * Parameters:
     * location - The new location to store
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * getDescription - This function returns the supplier's
     * description of the listing.
     *
     * Return - The description
     */
    public String getDescription() {
        return description;
    }

    /**
     * setDescription - This function replaces the supplier's
     * description.
     *
     * Parameters:
     * description - The new description to store
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * getSupplier - This function returns the owning supplier so
     * LST-01 ownership checks can compare ids.
     *
     * Return - The User who owns this listing
     */
    public User getSupplier() {
        return supplier;
    }

    /**
     * setSupplier - This function replaces the owning supplier.
     *
     * Parameters:
     * supplier - The User who should own this listing
     */
    public void setSupplier(User supplier) {
        this.supplier = supplier;
    }

    /**
     * isActive - This function reports whether the listing is still
     * offered. Deactivate sets this to false instead of deleting the
     * row.
     *
     * Return - True when the listing is active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * setActive - This function activates or deactivates the listing.
     *
     * Parameters:
     * active - True to offer the listing, false to hide it
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
