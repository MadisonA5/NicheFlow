/**
 * This program creates a PENDING booking for BK-01 and moves it
 * through the BK-02 lifecycle. Create names a listing and a UTC
 * window. PostgreSQL rejects a second overlapping insert. Confirm
 * charges the mocked Stripe layer once, then moves pending to
 * confirmed. Cancel is allowed for the customer, the listing
 * supplier, or an admin, and stores the BK-04 refund on that save.
 * Complete is allowed for the supplier.
 *
 * File:   BookingService.java
 * Course: AUCSC 325
 *
 * References:
 * https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-EXCLUSION
 *     - Exclusion constraints
 * https://docs.stripe.com/api/idempotent_requests
 *     - Stripe idempotency keys
 */

package com.nicheflow.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicheflow.exception.BookingAccessDeniedException;
import com.nicheflow.exception.BookingConflictException;
import com.nicheflow.exception.BookingNotFoundException;
import com.nicheflow.exception.InvalidBookingException;
import com.nicheflow.exception.InvalidBookingTransitionException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.BookingStatus;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.payment.PaymentClient;
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

    static final String CHANGE_DENIED =
            "You are not allowed to change this booking";

    private final BookingRepository bookingRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PaymentClient paymentClient;

    /**
     * BookingService - This function stores the repositories and the
     * payment client Spring injects. Create inserts a pending
     * booking. Confirm charges, then moves that booking.
     *
     * Parameters:
     * bookingRepository - The bookings table access point
     * listingRepository - The listings table access point
     * userRepository - The users table access point
     * paymentClient - The mocked Stripe charge port
     */
    public BookingService(BookingRepository bookingRepository,
            ListingRepository listingRepository,
            UserRepository userRepository, PaymentClient paymentClient) {
        this.bookingRepository = bookingRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.paymentClient = paymentClient;
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
     * transition - This function moves one booking to nextStatus when
     * that edge is allowed and the caller is one of the parties for
     * that edge. Confirm charges in confirm, not here.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * bookingId - The booking to move
     * nextStatus - CONFIRMED, COMPLETED, or CANCELLED
     *
     * Return - The booking after the new status is stored
     *
     * Example:
     * The booking customer may move PENDING to CONFIRMED. A supplier
     * who tries that move throws BookingAccessDeniedException.
     */
    @Transactional
    public Booking transition(AuthenticatedUser caller, Long bookingId,
            BookingStatus nextStatus) {
        Booking booking = loadBooking(bookingId);

        // To refuse every edge outside the BK-02 lifecycle
        rejectIllegalTransition(booking.getStatus(), nextStatus);

        // To refuse a caller who is not a party on this edge
        rejectUnauthorizedTransition(caller, booking, nextStatus);

        // To store the next status on the loaded row
        booking.setStatus(nextStatus);

        return bookingRepository.save(booking);
    }

    /**
     * confirm - This function charges the listing price, then moves
     * the caller's own pending booking to confirmed. The idempotency
     * key is stable for this booking, so a retry does not charge
     * again.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * bookingId - The pending booking to confirm
     *
     * Return - The booking after it is stored as confirmed
     */
    @Transactional
    public Booking confirm(AuthenticatedUser caller, Long bookingId) {
        Booking booking = loadBooking(bookingId);

        // To charge only a pending booking this customer may confirm
        rejectIllegalTransition(booking.getStatus(),
                BookingStatus.CONFIRMED);
        rejectUnauthorizedTransition(caller, booking,
                BookingStatus.CONFIRMED);

        // To take payment before the status leaves pending
        paymentClient.charge(booking.getListing().getPrice(),
                confirmIdempotencyKey(bookingId));

        return transition(caller, bookingId, BookingStatus.CONFIRMED);
    }

    /**
     * cancel - This function moves a pending or confirmed booking to
     * cancelled and stores the refund on that same save. The caller
     * must be the customer, the listing supplier, or an admin.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * bookingId - The booking to cancel
     *
     * Return - The booking stored as cancelled, with its refund
     */
    @Transactional
    public Booking cancel(AuthenticatedUser caller, Long bookingId) {
        Booking booking = loadBooking(bookingId);

        // To reuse the same edge and party checks as transition
        rejectIllegalTransition(booking.getStatus(),
                BookingStatus.CANCELLED);
        rejectUnauthorizedTransition(caller, booking,
                BookingStatus.CANCELLED);

        // To price the refund from the moment of this cancel
        BigDecimal refund = RefundCalculator.calculateRefund(
                booking.getListing().getPrice(),
                booking.getStartTime(),
                Instant.now());

        // To store the refund and the cancelled status together
        booking.setRefundAmount(refund);
        booking.setStatus(BookingStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    /**
     * confirmIdempotencyKey - This function builds the Stripe key for
     * one booking confirm. Retries of that confirm reuse it.
     *
     * Parameters:
     * bookingId - The booking being confirmed
     *
     * Return - The key, shaped as booking-{id}-confirm
     */
    static String confirmIdempotencyKey(Long bookingId) {
        return "booking-" + bookingId + "-confirm";
    }

    /**
     * rejectUnauthorizedTransition - This function throws when the
     * caller may not make this already-legal move. Confirm is the
     * booking customer. Cancel is that customer, the listing
     * supplier, or an admin. Complete is the listing supplier.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being moved
     * next - The status the caller asked for
     */
    private void rejectUnauthorizedTransition(AuthenticatedUser caller,
            Booking booking, BookingStatus next) {
        if (!callerMayTransition(caller, booking, next)) {
            throw new BookingAccessDeniedException(CHANGE_DENIED);
        }
    }

    /**
     * callerMayTransition - This function reports whether this caller
     * is a party allowed to make the move.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being moved
     * next - The status the caller asked for
     *
     * Return - True when the caller may make that move
     */
    private boolean callerMayTransition(AuthenticatedUser caller,
            Booking booking, BookingStatus next) {
        if (next == BookingStatus.CONFIRMED) {
            return isBookingCustomer(caller, booking);
        }

        if (next == BookingStatus.CANCELLED) {
            return callerMayCancel(caller, booking);
        }

        if (next == BookingStatus.COMPLETED) {
            return isListingSupplier(caller, booking);
        }

        return false;
    }

    /**
     * callerMayCancel - This function reports whether the caller is
     * the customer, the listing supplier, or an admin.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being cancelled
     *
     * Return - True when one of those three parties is calling
     */
    private boolean callerMayCancel(AuthenticatedUser caller,
            Booking booking) {
        if (isBookingCustomer(caller, booking)) {
            return true;
        }

        if (isListingSupplier(caller, booking)) {
            return true;
        }

        return caller.role() == Role.ADMIN;
    }

    /**
     * isBookingCustomer - This function compares the JWT id to the
     * customer stored on the booking.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being moved
     *
     * Return - True when the caller is that customer
     */
    private boolean isBookingCustomer(AuthenticatedUser caller,
            Booking booking) {
        return caller.id().equals(booking.getCustomer().getId());
    }

    /**
     * isListingSupplier - This function compares the JWT id to the
     * supplier who owns the booked listing.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being moved
     *
     * Return - True when the caller owns that listing
     */
    private boolean isListingSupplier(AuthenticatedUser caller,
            Booking booking) {
        Long supplierId = booking.getListing().getSupplier().getId();
        return caller.id().equals(supplierId);
    }

    /**
     * rejectIllegalTransition - This function throws when next is not
     * an allowed successor of current. Staying in the same status is
     * refused, as is every move out of completed or cancelled.
     *
     * Parameters:
     * current - The status stored on the booking
     * next - The status the caller asked for
     */
    private void rejectIllegalTransition(BookingStatus current,
            BookingStatus next) {
        if (!isAllowedTransition(current, next)) {
            throw new InvalidBookingTransitionException(current, next);
        }
    }

    /**
     * isAllowedTransition - This function reports whether the BK-02
     * lifecycle allows the move. The allowed edges are pending to
     * confirmed, pending or confirmed to cancelled, and confirmed
     * to completed.
     *
     * Parameters:
     * current - The status stored on the booking
     * next - The status the caller asked for
     *
     * Return - True when the move is one of those four edges
     */
    private static boolean isAllowedTransition(BookingStatus current,
            BookingStatus next) {
        if (current == BookingStatus.PENDING
                && (next == BookingStatus.CONFIRMED
                        || next == BookingStatus.CANCELLED)) {
            return true;
        }

        if (current == BookingStatus.CONFIRMED
                && (next == BookingStatus.COMPLETED
                        || next == BookingStatus.CANCELLED)) {
            return true;
        }

        return false;
    }

    /**
     * loadBooking - This function loads the booking row or throws
     * when the id is not stored.
     *
     * Parameters:
     * bookingId - The booking to load
     *
     * Return - The stored booking
     */
    private Booking loadBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(
                        bookingId));
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
