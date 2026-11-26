/**
 * This program implements BO-01 supplier blackouts: create, list,
 * and delete a UTC window a supplier marks unavailable on their own
 * listing. A blackout is rejected when it overlaps an existing
 * booking or an existing blackout on that listing. List is not
 * owner-scoped; any visitor may read the blackout windows the same
 * way booking availability is public.
 *
 * File:   ListingBlackoutService.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicheflow.exception.BlackoutConflictException;
import com.nicheflow.exception.BlackoutNotFoundException;
import com.nicheflow.exception.InvalidBlackoutException;
import com.nicheflow.exception.ListingAccessDeniedException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Listing;
import com.nicheflow.model.ListingBlackout;
import com.nicheflow.model.Role;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingBlackoutRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.security.AuthenticatedUser;

@Service
public class ListingBlackoutService {

    /**
     * See BookingService.OVERLAP_SQL_STATE / DEADLOCK_SQL_STATE for
     * why both SQLSTATEs mean this insert lost the window.
     */
    static final String OVERLAP_SQL_STATE = "23P01";

    static final String DEADLOCK_SQL_STATE = "40P01";

    static final String OVERLAP_CONSTRAINT =
            "ex_listing_blackouts_listing_window";

    private final ListingBlackoutRepository listingBlackoutRepository;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;

    /**
     * ListingBlackoutService - This function stores the repositories
     * Spring injects so create can check ownership, reject an
     * overlap with an existing booking or blackout, and persist.
     *
     * Parameters:
     * listingBlackoutRepository - The listing_blackouts table access
     *     point
     * listingRepository - The listings table access point
     * bookingRepository - The bookings table access point, used to
     *     reject a blackout that overlaps an active booking
     */
    public ListingBlackoutService(
            ListingBlackoutRepository listingBlackoutRepository,
            ListingRepository listingRepository,
            BookingRepository bookingRepository) {
        this.listingBlackoutRepository = listingBlackoutRepository;
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * create - This function inserts a blackout owned by the JWT
     * caller's listing. The window must move forward, must not
     * overlap an active booking on that listing, and must not
     * overlap a blackout already stored for that listing.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing to black out
     * startTime - The blackout start, a UTC instant
     * endTime - The blackout end, a UTC instant
     * reason - The supplier's optional note, or null
     *
     * Return - The persisted blackout
     */
    @Transactional
    public ListingBlackout create(AuthenticatedUser caller,
            Long listingId, Instant startTime, Instant endTime,
            String reason) {
        // To reject a window the check constraint would fail anyway
        rejectWindowThatDoesNotMoveForward(startTime, endTime);

        // To reject a customer or admin, then a supplier who does
        // not own this listing. Locked so a concurrent booking create
        // on the same listing cannot pass its overlap check before
        // this blackout commits, and vice versa.
        Listing listing = requireOwnedListingForUpdate(caller, listingId);

        // To keep an already-booked window out of a new blackout
        rejectOverlapWithBooking(listingId, startTime, endTime);

        // To reject a window that overlaps a blackout already stored
        rejectOverlapWithBlackout(listingId, startTime, endTime);

        ListingBlackout blackout = new ListingBlackout(listing,
                startTime, endTime, reason);

        return insertBlackout(blackout);
    }

    /**
     * listForListing - This function returns every blackout stored
     * for a listing, earliest first. Visitors may call it the same
     * way booking availability is public.
     *
     * Parameters:
     * listingId - The listing whose blackouts are requested
     *
     * Return - The blackouts for that listing, possibly empty
     */
    @Transactional(readOnly = true)
    public List<ListingBlackout> listForListing(Long listingId) {
        return listingBlackoutRepository.findByListingId(listingId);
    }

    /**
     * delete - This function removes a blackout when the JWT caller
     * owns the listing it was stored against.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing the blackout is on
     * blackoutId - The blackout to remove
     */
    @Transactional
    public void delete(AuthenticatedUser caller, Long listingId,
            Long blackoutId) {
        requireOwnedListing(caller, listingId);

        ListingBlackout blackout = listingBlackoutRepository
                .findById(blackoutId)
                .orElseThrow(() -> new BlackoutNotFoundException(
                        blackoutId));

        // To refuse deleting a blackout that belongs to another
        // listing by id-guessing
        if (!blackout.getListing().getId().equals(listingId)) {
            throw new BlackoutNotFoundException(blackoutId);
        }

        listingBlackoutRepository.delete(blackout);
    }

    /**
     * rejectWindowThatDoesNotMoveForward - This function throws when
     * the end is not strictly after the start.
     *
     * Parameters:
     * startTime - The blackout start, a UTC instant
     * endTime - The blackout end, a UTC instant
     */
    private void rejectWindowThatDoesNotMoveForward(Instant startTime,
            Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidBlackoutException(
                    "endTime must be after startTime");
        }
    }

    /**
     * rejectOverlapWithBooking - This function throws when an active
     * booking on this listing overlaps the requested window.
     *
     * Parameters:
     * listingId - The listing being blacked out
     * startTime - The blackout start, a UTC instant
     * endTime - The blackout end, a UTC instant
     */
    private void rejectOverlapWithBooking(Long listingId,
            Instant startTime, Instant endTime) {
        boolean overlapsBooking = bookingRepository
                .findUnavailableBookingsByListingId(listingId).stream()
                .anyMatch(booking -> booking.getStartTime()
                        .isBefore(endTime)
                        && booking.getEndTime().isAfter(startTime));

        if (overlapsBooking) {
            throw new BlackoutConflictException(
                    "That window overlaps an existing booking");
        }
    }

    /**
     * rejectOverlapWithBlackout - This function throws when a
     * blackout already stored for this listing overlaps the
     * requested window.
     *
     * Parameters:
     * listingId - The listing being blacked out
     * startTime - The blackout start, a UTC instant
     * endTime - The blackout end, a UTC instant
     */
    private void rejectOverlapWithBlackout(Long listingId,
            Instant startTime, Instant endTime) {
        if (listingBlackoutRepository.existsOverlapping(listingId,
                startTime, endTime)) {
            throw new BlackoutConflictException();
        }
    }

    /**
     * insertBlackout - This function flushes a new blackout row so
     * the exclusion constraint runs before create returns. An
     * overlap or a deadlock on that insert becomes a blackout
     * conflict, the same way a lost booking race does.
     *
     * Parameters:
     * blackout - The blackout to persist
     *
     * Return - The persisted blackout
     */
    private ListingBlackout insertBlackout(ListingBlackout blackout) {
        try {
            return listingBlackoutRepository.saveAndFlush(blackout);
        } catch (DataIntegrityViolationException exception) {
            if (isLostBlackoutRace(exception)) {
                throw new BlackoutConflictException();
            }

            throw exception;
        } catch (PessimisticLockingFailureException exception) {
            if (isLostBlackoutRace(exception)) {
                throw new BlackoutConflictException();
            }

            throw exception;
        }
    }

    /**
     * isLostBlackoutRace - This function walks the exception cause
     * chain for the overlap SQLSTATE, a deadlock, or the blackout
     * exclusion constraint name.
     *
     * Parameters:
     * exception - The integrity failure from the insert
     *
     * Return - True when this insert lost the window
     */
    private boolean isLostBlackoutRace(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (signalsLostBlackoutRace(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    /**
     * signalsLostBlackoutRace - This function checks one throwable
     * for an exclusion failure, a deadlock, or the constraint name.
     *
     * Parameters:
     * exception - One link in the cause chain
     *
     * Return - True when this link means the insert lost
     */
    private boolean signalsLostBlackoutRace(Throwable exception) {
        if (exception instanceof SQLException sqlException) {
            String sqlState = sqlException.getSQLState();
            if (OVERLAP_SQL_STATE.equals(sqlState)
                    || DEADLOCK_SQL_STATE.equals(sqlState)) {
                return true;
            }
        }

        String message = exception.getMessage();
        if (message != null && message.contains(OVERLAP_CONSTRAINT)) {
            return true;
        }

        return false;
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
        return requireOwnedListing(caller, listingId,
                listingRepository.findById(listingId));
    }

    /**
     * requireOwnedListingForUpdate - This function is
     * requireOwnedListing, but loads the listing under the same
     * PESSIMISTIC_WRITE lock BookingService.loadBookableListing takes
     * on create, so a booking create and a blackout create on the
     * same listing serialize against each other's overlap check.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing to load
     *
     * Return - The listing the caller owns, locked for this
     *     transaction
     */
    private Listing requireOwnedListingForUpdate(AuthenticatedUser caller,
            Long listingId) {
        return requireOwnedListing(caller, listingId,
                listingRepository.findByIdForUpdate(listingId));
    }

    /**
     * requireOwnedListing - This function rejects anyone whose JWT id
     * is not the supplier id on the already-loaded listing.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing being loaded, for the not-found message
     * loadedListing - The result of looking that listing up
     *
     * Return - The listing the caller owns
     */
    private Listing requireOwnedListing(AuthenticatedUser caller,
            Long listingId, Optional<Listing> loadedListing) {
        // To reject a customer or admin before the ownership compare
        requireSupplierRole(caller);

        Listing listing = loadedListing.orElseThrow(
                () -> new ListingNotFoundException(listingId));

        // To compare the JWT subject to listings.supplier_id
        if (!caller.id().equals(listing.getSupplier().getId())) {
            throw new ListingAccessDeniedException();
        }

        return listing;
    }

    /**
     * requireSupplierRole - This function reads the role off the
     * same AuthenticatedUser /me uses and rejects any other role.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     */
    private void requireSupplierRole(AuthenticatedUser caller) {
        if (caller.role() != Role.SUPPLIER) {
            throw new ListingAccessDeniedException();
        }
    }
}
