/**
 * This program implements LST-01 create, edit, and deactivate, plus
 * LST-02 search of active listings. Write callers are the
 * AuthenticatedUser from the JWT. Search is not owner-scoped.
 *
 * File:   ListingService.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.service;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicheflow.exception.InvalidSearchException;
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

    static final int DEFAULT_PAGE = 0;
    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 100;

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
     * location - Where the listing is offered
     * description - The supplier's description of the listing
     *
     * Return - The persisted listing
     */
    @Transactional
    public Listing create(AuthenticatedUser caller, String title,
            String category, BigDecimal price, String location,
            String description) {
        // To reject a customer or admin creating a supplier listing
        requireSupplierRole(caller);

        // To attach the User row that matches the JWT subject
        User supplier = loadCallerAccount(caller);

        // To persist a new active listing owned by that supplier
        Listing listing = new Listing(title, category, price, location,
                description, supplier);

        return listingRepository.save(listing);
    }

    /**
     * update - This function overwrites title, category, price,
     * location, and description when the JWT caller is the listing's
     * own supplier.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing to edit
     * title - The new display name
     * category - The new category string
     * price - The new price with two decimal places
     * location - The new place the listing is offered
     * description - The new supplier description
     *
     * Return - The updated listing
     */
    @Transactional
    public Listing update(AuthenticatedUser caller, Long listingId,
            String title, String category, BigDecimal price,
            String location, String description) {
        Listing listing = requireOwnedListing(caller, listingId);

        // To replace only the editable fields, never owner or active
        listing.setTitle(title);
        listing.setCategory(category);
        listing.setPrice(price);
        listing.setLocation(location);
        listing.setDescription(description);

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
     * search - This function finds active listings by optional
     * keyword (title, case-insensitive) and exact category, then
     * applies an optional inclusive price range and one page of
     * results. Keyword and category normalization is unchanged.
     *
     * Parameters:
     * keyword - The title fragment the client typed, or null
     * category - The exact category the client typed, or null
     * minPrice - The inclusive lower price, or null
     * maxPrice - The inclusive upper price, or null
     * page - The zero-based page number, or null for the default
     * size - The page size, or null for the default
     *
     * Return - One page of matching active listings
     */
    @Transactional(readOnly = true)
    public Page<Listing> search(String keyword, String category,
            BigDecimal minPrice, BigDecimal maxPrice, Integer page,
            Integer size) {
        // To skip a filter when the client left it blank
        String titleKeyword = normalizeKeyword(keyword);
        String exactCategory = normalizeCategory(category);

        // To reject a range that can never match
        rejectInvertedPriceRange(minPrice, maxPrice);

        // To apply defaults and a stable id sort before paging
        Pageable pageable = toPageable(page, size);

        // To bind typed, non-null values even when a filter is omitted
        return listingRepository.searchActive(
                titleKeyword == null ? "" : titleKeyword,
                exactCategory == null ? "" : exactCategory,
                minPrice != null,
                minPrice == null ? BigDecimal.ZERO : minPrice,
                maxPrice != null,
                maxPrice == null ? BigDecimal.ZERO : maxPrice, pageable);
    }

    /**
     * rejectInvertedPriceRange - This function throws when both
     * bounds are present and minPrice is greater than maxPrice.
     *
     * Parameters:
     * minPrice - The inclusive lower price, or null
     * maxPrice - The inclusive upper price, or null
     */
    private void rejectInvertedPriceRange(BigDecimal minPrice,
            BigDecimal maxPrice) {
        if (minPrice != null && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidSearchException(
                    "minPrice cannot be greater than maxPrice");
        }
    }

    /**
     * toPageable - This function turns optional page and size into a
     * PageRequest. Missing or negative page becomes 0. Missing or
     * sub-1 size becomes 20. Size is capped at 100. Admin user and
     * booking lists call this same helper so they cannot drift to
     * different limits.
     *
     * Parameters:
     * page - The requested page number, or null
     * size - The requested page size, or null
     *
     * Return - The page request sorted by id ascending
     */
    public static Pageable toPageable(Integer page, Integer size) {
        int pageNumber = DEFAULT_PAGE;
        if (page != null && page >= 0) {
            pageNumber = page;
        }

        int pageSize = DEFAULT_PAGE_SIZE;
        if (size != null && size >= 1) {
            pageSize = size;
        }
        if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        return PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * normalizeKeyword - This function trims the title fragment and
     * escapes LIKE special characters. Blank input becomes null so
     * the query does not apply a title filter.
     *
     * Parameters:
     * keyword - The raw query-string value
     *
     * Return - The escaped fragment, or null
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return escapeLike(keyword.trim());
    }

    /**
     * normalizeCategory - This function trims the category. Blank
     * input becomes null so the query does not apply a category
     * filter. Case is left as typed because the match is exact.
     *
     * Parameters:
     * category - The raw query-string value
     *
     * Return - The trimmed category, or null
     */
    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        return category.trim();
    }

    /**
     * escapeLike - This function prefixes \\, %, and _ so a keyword
     * such as 100% does not become a wildcard match.
     *
     * Parameters:
     * rawKeyword - The already-trimmed title fragment
     *
     * Return - The fragment safe to embed in LIKE
     */
    private String escapeLike(String rawKeyword) {
        return rawKeyword.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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
