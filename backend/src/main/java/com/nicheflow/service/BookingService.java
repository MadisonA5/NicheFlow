/**
 * This program creates a PENDING booking for BK-01. The customer
 * names a listing and a UTC window. PostgreSQL rejects a second
 * insert that overlaps that window on the same listing, including
 * when both inserts are in flight.
 *
 * File:   BookingService.java
 * Course: AUCSC 325
 *
 * References:
 * https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-EXCLUSION
 *     - Exclusion constraints
 */

package com.nicheflow.service;

import java.sql.SQLException;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicheflow.exception.BookingAccessDeniedException;
import com.nicheflow.exception.BookingConflictException;
import com.nicheflow.exception.InvalidBookingException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.security.AuthenticatedUser;

@Service
public class BookingService {

    /**
     * SQLSTATE 23P01 is exclusion_violation. 40P01 is a deadlock.
     * Either one means this insert did not keep the window.
     */
    static final String OVERLAP_SQL_STATE = "23P01";

    static final String DEADLOCK_SQL_STATE = "40P01";

    static final String OVERLAP_CONSTRAINT = "ex_bookings_listing_window";

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    /**
     * BookingService - This function stores the repositories Spring
     * injects so create can load the caller and the listing, then
     * insert one pending booking.
     *
     * Parameters:
     * bookingRepository - The bookings table access point
     * listingRepository - The listings table access point
     * userRepository - The users table access point
     */
    public BookingService(BookingRepository bookingRepository,
            ListingRepository listingRepository,
            UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
    }

    /**
     * create - This function inserts a PENDING booking for the JWT
     * caller. The database exclusion constraint is what keeps two
     * overlapping requests from both committing.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * listingId - The listing to book
     * startTime - The slot start, a UTC instant
     * endTime - The slot end, a UTC instant
     *
     * Return - The persisted pending booking
     *
     * Example:
     * Two creates for listing 4 from 15:00Z to 16:00Z. One booking
     * is stored. The other throws BookingConflictException.
     */
    @Transactional
    public Booking create(AuthenticatedUser caller, Long listingId,
            Instant startTime, Instant endTime) {
        // To keep booking create on the customer role
        requireCustomerRole(caller);

        // To reject a window the bookings check constraint would fail
        rejectWindowThatDoesNotMoveForward(startTime, endTime);

        // To attach the User row that matches the JWT subject
        User customer = loadCallerAccount(caller);

        // To book only a listing that is still offered
        Listing listing = loadBookableListing(listingId);

        return insertPendingBooking(listing, customer, startTime,
                endTime);
    }

    /**
     * insertPendingBooking - This function flushes a new PENDING row
     * so the exclusion constraint runs before create returns. An
     * overlap or a deadlock on that insert becomes a booking
     * conflict. Any other integrity failure is left alone.
     *
     * Parameters:
     * listing - The listing being booked
     * customer - The customer who is booking it
     * startTime - The slot start, a UTC instant
     * endTime - The slot end, a UTC instant
     *
     * Return - The persisted pending booking
     */
    private Booking insertPendingBooking(Listing listing, User customer,
            Instant startTime, Instant endTime) {
        Booking booking = new Booking(listing, customer, startTime,
                endTime);

        try {
            // To force the INSERT now, while this transaction is open
            return bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException exception) {
            if (isLostBookingRace(exception)) {
                throw new BookingConflictException();
            }

            throw exception;
        } catch (PessimisticLockingFailureException exception) {
            // To answer a deadlock on the exclusion index with 409
            if (isLostBookingRace(exception)) {
                throw new BookingConflictException();
            }

            throw exception;
        }
    }

    /**
     * isLostBookingRace - This function walks the exception cause
     * chain for the overlap SQLSTATE, a deadlock, or the BK-01
     * constraint name.
     *
     * Parameters:
     * exception - The integrity failure from the insert
     *
     * Return - True when this insert lost the window
     */
    private boolean isLostBookingRace(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (signalsLostBookingRace(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    /**
     * signalsLostBookingRace - This function checks one throwable for
     * an exclusion failure, a deadlock, or the constraint name.
     *
     * Parameters:
     * exception - One link in the cause chain
     *
     * Return - True when this link means the insert lost
     */
    private boolean signalsLostBookingRace(Throwable exception) {
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
     * rejectWindowThatDoesNotMoveForward - This function throws when
     * the end is not strictly after the start.
     *
     * Parameters:
     * startTime - The slot start, a UTC instant
     * endTime - The slot end, a UTC instant
     */
    private void rejectWindowThatDoesNotMoveForward(Instant startTime,
            Instant endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new InvalidBookingException(
                    "endTime must be after startTime");
        }
    }

    /**
     * loadBookableListing - This function loads the listing and
     * rejects one that has been deactivated.
     *
     * Parameters:
     * listingId - The listing the customer asked to book
     *
     * Return - The active listing
     */
    private Listing loadBookableListing(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(
                        listingId));

        if (!listing.isActive()) {
            throw new InvalidBookingException(
                    "This listing is not available");
        }

        return listing;
    }

    /**
     * requireCustomerRole - This function reads the role off the same
     * AuthenticatedUser /me uses and rejects any other role.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     */
    private void requireCustomerRole(AuthenticatedUser caller) {
        if (caller.role() != Role.CUSTOMER) {
            throw new BookingAccessDeniedException();
        }
    }

    /**
     * loadCallerAccount - This function loads the User row for the
     * JWT subject so the booking FK points at a real account.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     *
     * Return - The matching User entity
     */
    private User loadCallerAccount(AuthenticatedUser caller) {
        return userRepository.findById(caller.id())
                .orElseThrow(BookingAccessDeniedException::new);
    }
}
