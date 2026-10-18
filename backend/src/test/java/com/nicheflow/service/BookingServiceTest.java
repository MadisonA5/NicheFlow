/**
 * This program proves BK-01 create in the service: the JWT caller is
 * the customer, the row starts PENDING, a bad window is rejected, and
 * an exclusion-constraint failure becomes a booking conflict.
 *
 * File:   BookingServiceTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.exception.BookingAccessDeniedException;
import com.nicheflow.exception.BookingConflictException;
import com.nicheflow.exception.InvalidBookingException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.BookingStatus;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant START = Instant.parse(
            "2026-11-01T15:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-01T16:00:00Z");

    private final User customer = TestUsers.withId(9L, "bob@example.com",
            "$2a$10$hashed", Role.CUSTOMER);
    private final User supplier = TestUsers.withId(7L, "ada@example.com",
            "$2a$10$hashed", Role.SUPPLIER);
    private final AuthenticatedUser customerCaller = new AuthenticatedUser(
            9L, "bob@example.com", Role.CUSTOMER);
    private final AuthenticatedUser supplierCaller = new AuthenticatedUser(
            7L, "ada@example.com", Role.SUPPLIER);
    private final Listing listing = TestListings.withId(3L, "Kayak",
            "SPORTS", new BigDecimal("25.00"), supplier);

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingService bookingService;

    @Captor
    private ArgumentCaptor<Booking> savedBooking;

    /**
     * createStoresAPendingBookingForTheCustomer - This function
     * checks that the saved row is pending, owned by the JWT user,
     * and tied to the requested listing and window.
     */
    @Test
    void createStoresAPendingBookingForTheCustomer() {
        given(userRepository.findById(9L))
                .willReturn(Optional.of(customer));
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(listing));
        given(bookingRepository.saveAndFlush(any(Booking.class)))
                .willAnswer(call -> call.getArgument(0));

        Booking stored = bookingService.create(customerCaller, 3L, START,
                END);

        verify(bookingRepository).saveAndFlush(savedBooking.capture());
        assertThat(savedBooking.getValue().getCustomer())
                .isSameAs(customer);
        assertThat(savedBooking.getValue().getListing()).isSameAs(listing);
        assertThat(savedBooking.getValue().getStatus())
                .isEqualTo(BookingStatus.PENDING);
        assertThat(savedBooking.getValue().getStartTime()).isEqualTo(START);
        assertThat(savedBooking.getValue().getEndTime()).isEqualTo(END);
        assertThat(stored.getStatus()).isEqualTo(BookingStatus.PENDING);
    }

    /**
     * createRejectsASupplierCaller - This function proves a SUPPLIER
     * JWT cannot open a booking.
     */
    @Test
    void createRejectsASupplierCaller() {
        assertThatThrownBy(() -> bookingService.create(supplierCaller,
                3L, START, END))
                .isInstanceOf(BookingAccessDeniedException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    /**
     * createRejectsAMissingListing - This function proves an unknown
     * listing id does not insert a row.
     */
    @Test
    void createRejectsAMissingListing() {
        given(userRepository.findById(9L))
                .willReturn(Optional.of(customer));
        given(listingRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.create(customerCaller,
                99L, START, END))
                .isInstanceOf(ListingNotFoundException.class);

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    /**
     * createRejectsAnInactiveListing - This function proves a
     * deactivated listing cannot be booked.
     */
    @Test
    void createRejectsAnInactiveListing() {
        listing.setActive(false);
        given(userRepository.findById(9L))
                .willReturn(Optional.of(customer));
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(listing));

        assertThatThrownBy(() -> bookingService.create(customerCaller,
                3L, START, END))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessage("This listing is not available");

        verify(bookingRepository, never()).saveAndFlush(any());
    }

    /**
     * createRejectsAWindowThatDoesNotMoveForward - This function
     * proves an end at or before the start never reaches the insert.
     */
    @Test
    void createRejectsAWindowThatDoesNotMoveForward() {
        assertThatThrownBy(() -> bookingService.create(customerCaller,
                3L, START, START))
                .isInstanceOf(InvalidBookingException.class)
                .hasMessage("endTime must be after startTime");

        verify(bookingRepository, never()).saveAndFlush(any());
        verify(listingRepository, never()).findById(any());
    }

    /**
     * createTurnsAnOverlapViolationIntoAConflict - This function
     * proves SQLSTATE 23P01, the exclusion constraint, becomes a
     * booking conflict instead of a raw integrity error.
     */
    @Test
    void createTurnsAnOverlapViolationIntoAConflict() {
        givenReadyListing();
        given(bookingRepository.saveAndFlush(any(Booking.class)))
                .willThrow(overlapViolation());

        assertThatThrownBy(() -> bookingService.create(customerCaller,
                3L, START, END))
                .isInstanceOf(BookingConflictException.class);
    }

    /**
     * createTurnsADeadlockIntoAConflict - This function proves a
     * lock failure on the insert, SQLSTATE 40P01, is also a conflict.
     * PostgreSQL can report the losing racer that way.
     */
    @Test
    void createTurnsADeadlockIntoAConflict() {
        givenReadyListing();
        SQLException deadlock = new SQLException("deadlock detected",
                BookingService.DEADLOCK_SQL_STATE);
        given(bookingRepository.saveAndFlush(any(Booking.class)))
                .willThrow(new CannotAcquireLockException("deadlock",
                        deadlock));

        assertThatThrownBy(() -> bookingService.create(customerCaller,
                3L, START, END))
                .isInstanceOf(BookingConflictException.class);
    }

    /**
     * createRethrowsADifferentIntegrityFailure - This function proves
     * a foreign-key failure is not reported as an overlap.
     */
    @Test
    void createRethrowsADifferentIntegrityFailure() {
        givenReadyListing();
        SQLException foreignKey = new SQLException("fk", "23503");
        given(bookingRepository.saveAndFlush(any(Booking.class)))
                .willThrow(new DataIntegrityViolationException("fk",
                        foreignKey));

        assertThatThrownBy(() -> bookingService.create(customerCaller,
                3L, START, END))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * givenReadyListing - This function stubs the customer and an
     * active listing so the insert is the next step.
     */
    private void givenReadyListing() {
        given(userRepository.findById(9L))
                .willReturn(Optional.of(customer));
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(listing));
    }

    /**
     * overlapViolation - This function builds the integrity error
     * PostgreSQL raises for ex_bookings_listing_window.
     *
     * Return - A violation whose cause uses SQLSTATE 23P01
     */
    private static DataIntegrityViolationException overlapViolation() {
        SQLException overlap = new SQLException(
                "conflicting key value violates exclusion constraint \""
                        + BookingService.OVERLAP_CONSTRAINT + "\"",
                BookingService.OVERLAP_SQL_STATE);

        return new DataIntegrityViolationException("overlap", overlap);
    }
}
