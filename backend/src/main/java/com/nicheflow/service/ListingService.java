/**
 * This program implements LST-01 create, edit, and deactivate. The
 * caller is always the AuthenticatedUser parsed from the JWT, the
 * same principal /api/auth/me uses. Ownership is that principal's
 * id compared to listings.supplier_id.
 *
 * File:   ListingService.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicheflow.exception.ListingAccessDeniedException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.security.AuthenticatedUser;

@Service
public class ListingService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    /**
     * ListingService - This function stores the listing and user
     * repositories Spring injects so create can attach a real User
     * row as the supplier.
     *
     * Parameters:
     * listingRepository - The listings table access point
     * userRepository - The users table access point
     */
    public ListingService(ListingRepository listingRepository,
            UserRepository userRepository) {
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    /**
     * create - This function inserts a listing owned by the JWT
     * caller. The supplier is the User loaded by caller.id(), not a
     * client-supplied id.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * title - The display name of the listing
     * category - The category string
     * price - The price with two decimal places
     *
     * Return - The persisted listing
     */
    @Transactional
    public Listing create(AuthenticatedUser caller, String title,
            String category, BigDecimal price) {
        // To reject a customer or admin creating a supplier listing
        requireSupplierRole(caller);

        // To attach the User row that matches the JWT subject
        User supplier = loadCallerAccount(caller);

        // To persist a new active listing owned by that supplier
        Listing listing = new Listing(title, category, price, supplier);

        return listingRepository.save(listing);
    }

    /**
     * update - This function overwrites title, category, and price
     * when the JWT caller is the listing's own supplier.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing to edit
     * title - The new display name
     * category - The new category string
     * price - The new price with two decimal places
     *
     * Return - The updated listing
     */
    @Transactional
    public Listing update(AuthenticatedUser caller, Long listingId,
            String title, String category, BigDecimal price) {
        Listing listing = requireOwnedListing(caller, listingId);

        // To replace only the editable fields, never owner or active
        listing.setTitle(title);
        listing.setCategory(category);
        listing.setPrice(price);

        return listingRepository.save(listing);
    }

    /**
     * deactivate - This function sets active to false and leaves the
     * row in place. Only the listing's own supplier may do this.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing to hide
     *
     * Return - The listing with active false
     */
    @Transactional
    public Listing deactivate(AuthenticatedUser caller, Long listingId) {
        Listing listing = requireOwnedListing(caller, listingId);

        // To hide the listing without deleting its row
        listing.setActive(false);

        return listingRepository.save(listing);
    }

    /**
     * requireOwnedListing - This function loads the listing and
     * rejects anyone whose JWT id is not the supplier id.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing to load
     *
     * Return - The listing the caller owns
     */
    private Listing requireOwnedListing(AuthenticatedUser caller,
            Long listingId) {
        // To reject a customer or admin before the ownership compare
        requireSupplierRole(caller);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        // To compare the JWT subject to listings.supplier_id
        if (!caller.id().equals(listing.getSupplier().getId())) {
            throw new ListingAccessDeniedException();
        }

        return listing;
    }

    /**
     * requireSupplierRole - This function reads the role off the same
     * AuthenticatedUser /me uses and rejects any other role.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     */
    private void requireSupplierRole(AuthenticatedUser caller) {
        if (caller.role() != Role.SUPPLIER) {
            throw new ListingAccessDeniedException();
        }
    }

    /**
     * loadCallerAccount - This function loads the User row for the
     * JWT subject so the listing FK points at a real account.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     *
     * Return - The matching User entity
     */
    private User loadCallerAccount(AuthenticatedUser caller) {
        return userRepository.findById(caller.id())
                .orElseThrow(ListingAccessDeniedException::new);
    }
}
