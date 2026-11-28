/**
 * This program implements ADM-02 reviews: a customer or a listing
 * supplier may each leave one review of the other party after their
 * shared booking reaches completed. List is not party-scoped; any
 * visitor may read the reviews on a booking or a listing the same
 * way booking availability is public.
 *
 * File:   ReviewService.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicheflow.exception.BookingNotFoundException;
import com.nicheflow.exception.InvalidReviewException;
import com.nicheflow.exception.ReviewAccessDeniedException;
import com.nicheflow.exception.ReviewConflictException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.BookingStatus;
import com.nicheflow.model.Review;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ReviewRepository;
import com.nicheflow.repository.ReviewRepository.UserRatingSummary;
import com.nicheflow.security.AuthenticatedUser;

@Service
public class ReviewService {

    /**
     * A plain unique-constraint violation, unlike the exclusion and
     * deadlock SQLSTATEs BookingService and ListingBlackoutService
     * watch for, since a review conflict is one reviewer re-reviewing
     * the same booking, not two insert races over a time window.
     */
    static final String UNIQUE_SQL_STATE = "23505";

    static final String UNIQUE_CONSTRAINT =
            "uq_reviews_booking_reviewer";

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    /**
     * ReviewService - This function stores the repositories Spring
     * injects so create can load the booking, check which party the
     * caller is, and persist.
     *
     * Parameters:
     * reviewRepository - The reviews table access point
     * bookingRepository - The bookings table access point, used to
     *     load the booking a review is about without going through
     *     BookingService
     */
    public ReviewService(ReviewRepository reviewRepository,
            BookingRepository bookingRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * create - This function inserts a review the JWT caller leaves
     * for the other party on a booking that has reached completed.
     * The reviewee is derived from which party the caller is; anyone
     * else is refused.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * bookingId - The completed booking being reviewed
     * rating - The 1-5 star rating
     * comment - The reviewer's optional note, or null
     *
     * Return - The persisted review
     */
    @Transactional
    public Review create(AuthenticatedUser caller, Long bookingId,
            int rating, String comment) {
        Booking booking = loadBooking(bookingId);

        // To refuse a stranger to this booking before telling them
        // anything about its lifecycle state
        User reviewer = resolveReviewer(caller, booking);

        // To reject a review attempted before the parties confirmed
        // the booking happened
        rejectBookingNotCompleted(booking);

        User reviewee = resolveReviewee(caller, booking);

        Review review = new Review(booking, reviewer, reviewee, rating,
                comment);

        return insertReview(review);
    }

    /**
     * listForBooking - This function returns every review stored for
     * a booking, earliest first. Visitors may call it the same way
     * booking availability is public.
     *
     * Parameters:
     * bookingId - The booking whose reviews are requested
     *
     * Return - The reviews for that booking, possibly empty
     */
    @Transactional(readOnly = true)
    public List<Review> listForBooking(Long bookingId) {
        return reviewRepository.findByBookingId(bookingId);
    }

    /**
     * listForListing - This function returns every review a
     * listing's supplier has received across that listing's
     * bookings, earliest first. A supplier's own review of a
     * customer is excluded, so it cannot be used to pad the
     * listing's public reputation.
     *
     * Parameters:
     * listingId - The listing whose reviews are requested
     *
     * Return - The reviews about that listing's supplier, possibly
     *     empty
     */
    @Transactional(readOnly = true)
    public List<Review> listForListing(Long listingId) {
        return reviewRepository.findByBookingListingId(listingId);
    }

    /**
     * customerRatingSummaries - This function returns the average
     * rating and review count received by every customer in the
     * given ids who has been reviewed at least once by someone other
     * than the viewing supplier, keyed by that customer's id. Used to
     * show a supplier a requesting customer's ADM-02 standing with
     * other suppliers, including on a still PENDING request, without
     * the supplier's own earlier review of that same customer
     * padding the number they see.
     *
     * Parameters:
     * customerIds - The customers to summarize
     * viewingSupplierId - The caller whose own reviews of these
     *     customers should not count toward the average
     *
     * Return - A map from customer id to their rating summary; a
     *     customer with no qualifying reviews has no entry
     */
    @Transactional(readOnly = true)
    public Map<Long, UserRatingSummary> customerRatingSummaries(
            List<Long> customerIds, Long viewingSupplierId) {
        if (customerIds.isEmpty()) {
            return Map.of();
        }

        return reviewRepository.summarizeByRevieweeIds(customerIds,
                viewingSupplierId)
                .stream()
                .collect(Collectors.toMap(
                        UserRatingSummary::getUserId,
                        Function.identity()));
    }

    /**
     * reviewedBookingIds - This function returns, among the given
     * booking ids, those the caller has already left a review on.
     * Used to show "Review" or "See reviews" on the account page for
     * every completed booking in one query instead of one per
     * booking.
     *
     * Parameters:
     * bookingIds - The bookings to check
     * reviewerId - The caller whose own reviews are being looked up
     *
     * Return - The booking ids the caller has already reviewed
     */
    @Transactional(readOnly = true)
    public Set<Long> reviewedBookingIds(List<Long> bookingIds,
            Long reviewerId) {
        if (bookingIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(reviewRepository.findReviewedBookingIds(
                bookingIds, reviewerId));
    }

    /**
     * rejectBookingNotCompleted - This function throws when the
     * booking has not reached completed, so ADM-02 timing holds.
     *
     * Parameters:
     * booking - The booking a review is being attempted on
     */
    private void rejectBookingNotCompleted(Booking booking) {
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new InvalidReviewException(
                    "Reviews are only allowed after a completed booking");
        }
    }

    /**
     * resolveReviewer - This function returns the User row matching
     * the caller, trusting the caller is already one of the two
     * parties by the time this runs.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being reviewed
     *
     * Return - The customer or the listing supplier, whichever the
     *     caller is
     */
    private User resolveReviewer(AuthenticatedUser caller,
            Booking booking) {
        if (BookingParties.isCustomer(caller, booking)) {
            return booking.getCustomer();
        }

        if (BookingParties.isSupplier(caller, booking)) {
            return booking.getListing().getSupplier();
        }

        throw new ReviewAccessDeniedException();
    }

    /**
     * resolveReviewee - This function returns the User row for the
     * party the caller is not, so a review is always about the other
     * side of the booking.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being reviewed
     *
     * Return - The customer or the listing supplier, whichever the
     *     caller is not
     */
    private User resolveReviewee(AuthenticatedUser caller,
            Booking booking) {
        if (BookingParties.isCustomer(caller, booking)) {
            return booking.getListing().getSupplier();
        }

        if (BookingParties.isSupplier(caller, booking)) {
            return booking.getCustomer();
        }

        throw new ReviewAccessDeniedException();
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
     * insertReview - This function flushes a new review row so the
     * unique constraint runs before create returns. A second review
     * from the same reviewer on the same booking becomes a review
     * conflict.
     *
     * Parameters:
     * review - The review to persist
     *
     * Return - The persisted review
     */
    private Review insertReview(Review review) {
        try {
            return reviewRepository.saveAndFlush(review);
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateReview(exception)) {
                throw new ReviewConflictException();
            }

            throw exception;
        }
    }

    /**
     * isDuplicateReview - This function walks the exception cause
     * chain for the unique-violation SQLSTATE or the review unique
     * constraint name.
     *
     * Parameters:
     * exception - The integrity failure from the insert
     *
     * Return - True when this insert lost to an existing review
     */
    private boolean isDuplicateReview(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (signalsDuplicateReview(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    /**
     * signalsDuplicateReview - This function checks one throwable for
     * the unique-violation SQLSTATE or the constraint name.
     *
     * Parameters:
     * exception - One link in the cause chain
     *
     * Return - True when this link means the insert lost
     */
    private boolean signalsDuplicateReview(Throwable exception) {
        if (exception instanceof SQLException sqlException) {
            if (UNIQUE_SQL_STATE.equals(sqlException.getSQLState())) {
                return true;
            }
        }

        String message = exception.getMessage();
        if (message != null && message.contains(UNIQUE_CONSTRAINT)) {
            return true;
        }

        return false;
    }
}
