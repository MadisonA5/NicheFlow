/**
 * This program proves ADM-02 create and list in the service: either
 * party may review the other once the booking is completed, neither
 * party may review it earlier, a stranger is refused, and a
 * unique-constraint violation on the insert becomes a review
 * conflict.
 *
 * File:   ReviewServiceTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestReviews;
import com.nicheflow.TestUsers;
import com.nicheflow.exception.BookingNotFoundException;
import com.nicheflow.exception.InvalidReviewException;
import com.nicheflow.exception.ReviewAccessDeniedException;
import com.nicheflow.exception.ReviewConflictException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.BookingStatus;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Review;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ReviewRepository;
import com.nicheflow.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Instant START = Instant.parse(
            "2026-11-01T15:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-02T15:00:00Z");
    private static final long BOOKING_ID = 11L;

    private final User customer = TestUsers.withId(9L, "bob@example.com",
            "$2a$10$hashed", Role.CUSTOMER);
    private final User supplier = TestUsers.withId(7L, "ada@example.com",
            "$2a$10$hashed", Role.SUPPLIER);
    private final Listing listing = TestListings.withId(3L, "Kayak",
            "SPORTS", new BigDecimal("25.00"), supplier);
    private final AuthenticatedUser customerCaller = new AuthenticatedUser(
            9L, "bob@example.com", Role.CUSTOMER);
    private final AuthenticatedUser supplierCaller = new AuthenticatedUser(
            7L, "ada@example.com", Role.SUPPLIER);
    private final AuthenticatedUser strangerCaller = new AuthenticatedUser(
            8L, "other@example.com", Role.CUSTOMER);

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Captor
    private ArgumentCaptor<Review> savedReview;

    /**
     * customerCanReviewTheSupplierAfterCompletion - This function
     * checks the saved review names the supplier as reviewee.
     */
    @Test
    void customerCanReviewTheSupplierAfterCompletion() {
        Booking booking = load(BookingStatus.COMPLETED);
        given(reviewRepository.saveAndFlush(any(Review.class)))
                .willAnswer(call -> call.getArgument(0));

        reviewService.create(customerCaller, BOOKING_ID, 5, "Great stay");

        verify(reviewRepository).saveAndFlush(savedReview.capture());
        assertThat(savedReview.getValue().getBooking()).isSameAs(booking);
        assertThat(savedReview.getValue().getReviewer()).isSameAs(customer);
        assertThat(savedReview.getValue().getReviewee()).isSameAs(supplier);
        assertThat(savedReview.getValue().getRating()).isEqualTo(5);
        assertThat(savedReview.getValue().getComment())
                .isEqualTo("Great stay");
    }

    /**
     * supplierCanReviewTheCustomerAfterCompletion - This function
     * checks the saved review names the customer as reviewee.
     */
    @Test
    void supplierCanReviewTheCustomerAfterCompletion() {
        load(BookingStatus.COMPLETED);
        given(reviewRepository.saveAndFlush(any(Review.class)))
                .willAnswer(call -> call.getArgument(0));

        reviewService.create(supplierCaller, BOOKING_ID, 4, null);

        verify(reviewRepository).saveAndFlush(savedReview.capture());
        assertThat(savedReview.getValue().getReviewer()).isSameAs(supplier);
        assertThat(savedReview.getValue().getReviewee()).isSameAs(customer);
    }

    /**
     * creatingAReviewBeforeCompletionIsRejected - This function proves
     * a confirmed booking is not yet reviewable.
     */
    @Test
    void creatingAReviewBeforeCompletionIsRejected() {
        load(BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> reviewService.create(customerCaller,
                BOOKING_ID, 5, null))
                .isInstanceOf(InvalidReviewException.class);

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    /**
     * callerWhoIsNeitherPartyIsRejected - This function proves a
     * stranger to the booking cannot leave a review.
     */
    @Test
    void callerWhoIsNeitherPartyIsRejected() {
        load(BookingStatus.COMPLETED);

        assertThatThrownBy(() -> reviewService.create(strangerCaller,
                BOOKING_ID, 5, null))
                .isInstanceOf(ReviewAccessDeniedException.class);

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    /**
     * strangerIsRejectedBeforeLearningTheBookingIsNotCompleted - This
     * function proves a non-party caller gets 403 even when the
     * booking has not reached completed, so they never learn the
     * booking's lifecycle state.
     */
    @Test
    void strangerIsRejectedBeforeLearningTheBookingIsNotCompleted() {
        load(BookingStatus.CONFIRMED);

        assertThatThrownBy(() -> reviewService.create(strangerCaller,
                BOOKING_ID, 5, null))
                .isInstanceOf(ReviewAccessDeniedException.class);

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    /**
     * missingBookingIdOnCreateIsNotFound - This function proves an
     * unknown booking id is 404, not a silent create.
     */
    @Test
    void missingBookingIdOnCreateIsNotFound() {
        given(bookingRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(customerCaller,
                99L, 5, null))
                .isInstanceOf(BookingNotFoundException.class);

        verify(reviewRepository, never()).saveAndFlush(any(Review.class));
    }

    /**
     * secondReviewFromTheSameReviewerOnTheSameBookingIsAConflict -
     * This function proves SQLSTATE 23505, the unique constraint,
     * becomes a review conflict instead of a raw integrity error.
     */
    @Test
    void secondReviewFromTheSameReviewerOnTheSameBookingIsAConflict() {
        load(BookingStatus.COMPLETED);
        SQLException duplicate = new SQLException(
                "duplicate key value violates unique constraint \""
                        + ReviewService.UNIQUE_CONSTRAINT + "\"",
                ReviewService.UNIQUE_SQL_STATE);
        given(reviewRepository.saveAndFlush(any(Review.class)))
                .willThrow(new DataIntegrityViolationException("dup",
                        duplicate));

        assertThatThrownBy(() -> reviewService.create(customerCaller,
                BOOKING_ID, 5, null))
                .isInstanceOf(ReviewConflictException.class);
    }

    /**
     * createRethrowsADifferentIntegrityFailure - This function proves
     * a foreign-key failure is not reported as a duplicate review.
     */
    @Test
    void createRethrowsADifferentIntegrityFailure() {
        load(BookingStatus.COMPLETED);
        SQLException foreignKey = new SQLException("fk", "23503");
        given(reviewRepository.saveAndFlush(any(Review.class)))
                .willThrow(new DataIntegrityViolationException("fk",
                        foreignKey));

        assertThatThrownBy(() -> reviewService.create(customerCaller,
                BOOKING_ID, 5, null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * customerRatingSummariesAveragesAcrossReviewsReceived - This
     * function proves the summary map is keyed by customer id.
     */
    @Test
    void customerRatingSummariesAveragesAcrossReviewsReceived() {
        ReviewRepository.UserRatingSummary summary =
                new ReviewRepository.UserRatingSummary() {
            @Override
            public Long getUserId() {
                return 9L;
            }

            @Override
            public Double getAverageRating() {
                return 3.5;
            }

            @Override
            public Long getReviewCount() {
                return 2L;
            }
        };
        given(reviewRepository.summarizeByRevieweeIds(List.of(9L), 7L))
                .willReturn(List.of(summary));

        assertThat(reviewService.customerRatingSummaries(List.of(9L), 7L))
                .containsOnlyKeys(9L);
        assertThat(reviewService.customerRatingSummaries(List.of(9L), 7L)
                .get(9L).getAverageRating()).isEqualTo(3.5);
    }

    /**
     * customerRatingSummariesExcludesTheViewingSuppliersOwnReview -
     * This function proves the viewing supplier's own review of a
     * customer is passed to the repository as the id to exclude, so
     * it cannot be reflected back at them as independent standing.
     */
    @Test
    void customerRatingSummariesExcludesTheViewingSuppliersOwnReview() {
        given(reviewRepository.summarizeByRevieweeIds(List.of(9L), 7L))
                .willReturn(List.of());

        reviewService.customerRatingSummaries(List.of(9L), 7L);

        verify(reviewRepository).summarizeByRevieweeIds(List.of(9L), 7L);
    }

    /**
     * customerRatingSummariesSkipsTheQueryForNoCustomers - This
     * function proves an empty id list never reaches the repository.
     */
    @Test
    void customerRatingSummariesSkipsTheQueryForNoCustomers() {
        assertThat(reviewService.customerRatingSummaries(List.of(), 7L))
                .isEmpty();

        verify(reviewRepository, never())
                .summarizeByRevieweeIds(any(), any());
    }

    /**
     * reviewedBookingIdsReturnsOnlyTheCallersOwnReviews - This
     * function proves the lookup is scoped to the given reviewer.
     */
    @Test
    void reviewedBookingIdsReturnsOnlyTheCallersOwnReviews() {
        given(reviewRepository.findReviewedBookingIds(List.of(11L, 12L), 9L))
                .willReturn(List.of(11L));

        assertThat(reviewService.reviewedBookingIds(List.of(11L, 12L), 9L))
                .containsExactly(11L);
    }

    /**
     * reviewedBookingIdsSkipsTheQueryForNoBookings - This function
     * proves an empty id list never reaches the repository.
     */
    @Test
    void reviewedBookingIdsSkipsTheQueryForNoBookings() {
        assertThat(reviewService.reviewedBookingIds(List.of(), 9L))
                .isEmpty();

        verify(reviewRepository, never())
                .findReviewedBookingIds(any(), any());
    }

    /**
     * listForBookingReturnsBothDirectionsWhenPresent - This function
     * proves list is a plain passthrough with no auth gate.
     */
    @Test
    void listForBookingReturnsBothDirectionsWhenPresent() {
        Booking booking = TestBookings.withId(BOOKING_ID, listing,
                customer, START, END);
        Review fromCustomer = TestReviews.withId(21L, booking, customer,
                supplier, 5, "Great stay");
        Review fromSupplier = TestReviews.withId(22L, booking, supplier,
                customer, 4, null);
        given(reviewRepository.findByBookingId(BOOKING_ID))
                .willReturn(List.of(fromCustomer, fromSupplier));

        assertThat(reviewService.listForBooking(BOOKING_ID))
                .containsExactly(fromCustomer, fromSupplier);
    }

    /**
     * listForListingReturnsReviewsAcrossBookingsOnThatListing - This
     * function proves the listing-scoped read delegates to the
     * listing-id query.
     */
    @Test
    void listForListingReturnsReviewsAcrossBookingsOnThatListing() {
        Booking booking = TestBookings.withId(BOOKING_ID, listing,
                customer, START, END);
        Review review = TestReviews.withId(21L, booking, customer,
                supplier, 5, "Great stay");
        given(reviewRepository.findByBookingListingId(3L))
                .willReturn(List.of(review));

        assertThat(reviewService.listForListing(3L))
                .containsExactly(review);
    }

    /**
     * load - This function stubs a booking in the given status.
     *
     * Parameters:
     * status - The lifecycle state to stamp on the booking
     *
     * Return - The booking with id 11
     */
    private Booking load(BookingStatus status) {
        Booking booking = TestBookings.withId(BOOKING_ID, listing,
                customer, START, END);
        booking.setStatus(status);
        given(bookingRepository.findById(BOOKING_ID))
                .willReturn(Optional.of(booking));
        return booking;
    }
}
