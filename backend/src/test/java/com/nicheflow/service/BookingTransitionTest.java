/**
 * This program proves the BK-02 lifecycle in BookingService. Each
 * allowed edge is stored. Every other pair of statuses is refused,
 * and the row is left unchanged.
 *
 * File:   BookingTransitionTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.nicheflow.model.BookingStatus.CANCELLED;
import static com.nicheflow.model.BookingStatus.COMPLETED;
import static com.nicheflow.model.BookingStatus.CONFIRMED;
import static com.nicheflow.model.BookingStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.exception.BookingNotFoundException;
import com.nicheflow.exception.InvalidBookingTransitionException;
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

@ExtendWith(MockitoExtension.class)
class BookingTransitionTest {

    private static final Instant START = Instant.parse(
            "2026-11-01T15:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-01T16:00:00Z");
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

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentClient paymentClient;

    @InjectMocks
    private BookingService bookingService;

    /**
     * transitionStoresEachValidNextStatus - This function stores
     * pending to confirmed, pending or confirmed to cancelled, and
     * confirmed to completed.
     *
     * Parameters:
     * current - The status the booking starts in
     * next - The status the caller asked for
     */
    @ParameterizedTest
    @MethodSource("validTransitions")
    void transitionStoresEachValidNextStatus(BookingStatus current,
            BookingStatus next) {
        Booking booking = bookingIn(current);
        given(bookingRepository.findById(BOOKING_ID))
                .willReturn(Optional.of(booking));
        given(bookingRepository.save(any(Booking.class)))
                .willAnswer(call -> call.getArgument(0));

        Booking stored = bookingService.transition(allowedCaller(next),
                BOOKING_ID, next);

        assertThat(stored.getStatus()).isEqualTo(next);
        verify(bookingRepository).save(booking);
    }

    /**
     * transitionRejectsEachIllegalMove - This function refuses every
     * pair that is not one of the four BK-02 edges, and does not
     * save.
     *
     * Parameters:
     * current - The status the booking starts in
     * next - The status the caller asked for
     */
    @ParameterizedTest
    @MethodSource("illegalTransitions")
    void transitionRejectsEachIllegalMove(BookingStatus current,
            BookingStatus next) {
        Booking booking = bookingIn(current);
        given(bookingRepository.findById(BOOKING_ID))
                .willReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.transition(customerCaller,
                BOOKING_ID, next))
                .isInstanceOf(InvalidBookingTransitionException.class)
                .hasMessage("Cannot move a booking from " + current
                        + " to " + next);

        assertThat(booking.getStatus()).isEqualTo(current);
        verify(bookingRepository, never()).save(any());
    }

    /**
     * transitionRejectsAMissingBooking - This function proves an
     * unknown id does not become a status change.
     */
    @Test
    void transitionRejectsAMissingBooking() {
        given(bookingRepository.findById(BOOKING_ID))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.transition(customerCaller,
                BOOKING_ID, CONFIRMED))
                .isInstanceOf(BookingNotFoundException.class)
                .hasMessage("No booking found with id " + BOOKING_ID);

        verify(bookingRepository, never()).save(any());
    }

    /**
     * validTransitions - This function lists the allowed BK-02 edges
     * the service is allowed to store.
     *
     * Return - The booking lifecycle's allowed status edges
     */
    private static Stream<Arguments> validTransitions() {
        return Stream.of(
                Arguments.of(PENDING, CONFIRMED),
                Arguments.of(PENDING, BookingStatus.ACCEPTED),
                Arguments.of(PENDING, CANCELLED),
                Arguments.of(BookingStatus.ACCEPTED, CONFIRMED),
                Arguments.of(BookingStatus.ACCEPTED, CANCELLED),
                Arguments.of(CONFIRMED, CANCELLED),
                Arguments.of(CONFIRMED, COMPLETED));
    }

    /**
     * illegalTransitions - This function lists every other pair of
     * statuses, including staying put and leaving a terminal state.
     *
     * Return - The twelve refused moves
     */
    private static Stream<Arguments> illegalTransitions() {
        return Stream.of(
                Arguments.of(PENDING, PENDING),
                Arguments.of(PENDING, COMPLETED),
                Arguments.of(BookingStatus.ACCEPTED, PENDING),
                Arguments.of(BookingStatus.ACCEPTED,
                        BookingStatus.ACCEPTED),
                Arguments.of(BookingStatus.ACCEPTED, COMPLETED),
                Arguments.of(CONFIRMED, PENDING),
                Arguments.of(CONFIRMED, BookingStatus.ACCEPTED),
                Arguments.of(CONFIRMED, CONFIRMED),
                Arguments.of(COMPLETED, PENDING),
                Arguments.of(COMPLETED, BookingStatus.ACCEPTED),
                Arguments.of(COMPLETED, CONFIRMED),
                Arguments.of(COMPLETED, COMPLETED),
                Arguments.of(COMPLETED, CANCELLED),
                Arguments.of(CANCELLED, PENDING),
                Arguments.of(CANCELLED, BookingStatus.ACCEPTED),
                Arguments.of(CANCELLED, CONFIRMED),
                Arguments.of(CANCELLED, COMPLETED),
                Arguments.of(CANCELLED, CANCELLED));
    }

    /**
     * bookingIn - This function builds a stored booking already in
     * the given status.
     *
     * Parameters:
     * status - The lifecycle state to stamp on the booking
     *
     * Return - The booking with id 11
     */
    private Booking bookingIn(BookingStatus status) {
        Booking booking = TestBookings.withId(BOOKING_ID, listing,
                customer, START, END);
        booking.setStatus(status);
        return booking;
    }

    /**
     * allowedCaller - This function picks a party who may make the
     * move, so the lifecycle test is not rejected for authorization.
     * Accept and complete belong to the supplier. Confirm and cancel
     * belong to the customer.
     *
     * Parameters:
     * next - The status the caller asked for
     *
     * Return - The supplier for accept/complete, otherwise the customer
     */
    private AuthenticatedUser allowedCaller(BookingStatus next) {
        if (next == COMPLETED || next == BookingStatus.ACCEPTED) {
            return supplierCaller;
        }

        return customerCaller;
    }
}
