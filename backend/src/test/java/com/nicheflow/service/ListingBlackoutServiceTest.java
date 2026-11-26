/**
 * This program proves BO-01 create, list, and delete in the
 * service: role and ownership gates, a window that does not move
 * forward, an overlap with an existing booking or blackout, and that
 * delete never crosses into another listing's blackout.
 *
 * File:   ListingBlackoutServiceTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nicheflow.TestBlackouts;
import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.exception.BlackoutConflictException;
import com.nicheflow.exception.BlackoutNotFoundException;
import com.nicheflow.exception.InvalidBlackoutException;
import com.nicheflow.exception.ListingAccessDeniedException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.ListingBlackout;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingBlackoutRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class ListingBlackoutServiceTest {

    private static final Instant START = Instant.parse(
            "2026-11-01T15:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-02T15:00:00Z");

    private final User owner = TestUsers.withId(7L, "ada@example.com",
            "$2a$10$hashed", Role.SUPPLIER);
    private final User customer = TestUsers.withId(9L, "bob@example.com",
            "$2a$10$hashed", Role.CUSTOMER);
    private final AuthenticatedUser ownerCaller = new AuthenticatedUser(
            7L, "ada@example.com", Role.SUPPLIER);
    private final AuthenticatedUser otherSupplier = new AuthenticatedUser(
            8L, "other@example.com", Role.SUPPLIER);
    private final AuthenticatedUser customerCaller = new AuthenticatedUser(
            9L, "bob@example.com", Role.CUSTOMER);
    private final Listing listing = TestListings.withId(3L, "Kayak",
            "SPORTS", new BigDecimal("25.00"), owner);

    @Mock
    private ListingBlackoutRepository listingBlackoutRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private ListingBlackoutService blackoutService;

    @Captor
    private ArgumentCaptor<ListingBlackout> savedBlackout;

    /**
     * createPersistsBlackoutForOwner - This function checks that the
     * saved row is tied to the requested listing and window.
     */
    @Test
    void createPersistsBlackoutForOwner() {
        givenNoConflicts();
        given(listingBlackoutRepository.saveAndFlush(
                any(ListingBlackout.class)))
                .willAnswer(call -> call.getArgument(0));

        ListingBlackout stored = blackoutService.create(ownerCaller, 3L,
                START, END, "Vacation");

        verify(listingBlackoutRepository)
                .saveAndFlush(savedBlackout.capture());
        assertThat(savedBlackout.getValue().getListing()).isSameAs(listing);
        assertThat(savedBlackout.getValue().getStartTime())
                .isEqualTo(START);
        assertThat(savedBlackout.getValue().getEndTime()).isEqualTo(END);
        assertThat(stored.getReason()).isEqualTo("Vacation");
    }

    /**
     * createRejectsACustomerCaller - This function proves a CUSTOMER
     * JWT cannot create a blackout.
     */
    @Test
    void createRejectsACustomerCaller() {
        assertThatThrownBy(() -> blackoutService.create(customerCaller,
                3L, START, END, null))
                .isInstanceOf(ListingAccessDeniedException.class);

        verify(listingBlackoutRepository, never())
                .saveAndFlush(any(ListingBlackout.class));
    }

    /**
     * createRejectsADifferentSupplier - This function proves another
     * SUPPLIER JWT cannot black out Ada's listing.
     */
    @Test
    void createRejectsADifferentSupplier() {
        given(listingRepository.findByIdForUpdate(3L))
                .willReturn(Optional.of(listing));

        assertThatThrownBy(() -> blackoutService.create(otherSupplier,
                3L, START, END, null))
                .isInstanceOf(ListingAccessDeniedException.class);

        verify(listingBlackoutRepository, never())
                .saveAndFlush(any(ListingBlackout.class));
    }

    /**
     * createRejectsAMissingListing - This function proves an unknown
     * listing id is 404, not a silent create.
     */
    @Test
    void createRejectsAMissingListing() {
        given(listingRepository.findByIdForUpdate(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> blackoutService.create(ownerCaller, 99L,
                START, END, null))
                .isInstanceOf(ListingNotFoundException.class);
    }

    /**
     * createRejectsAWindowThatDoesNotMoveForward - This function
     * proves an end at or before the start never reaches ownership
     * or the insert.
     */
    @Test
    void createRejectsAWindowThatDoesNotMoveForward() {
        assertThatThrownBy(() -> blackoutService.create(ownerCaller, 3L,
                START, START, null))
                .isInstanceOf(InvalidBlackoutException.class);

        verify(listingRepository, never()).findByIdForUpdate(any());
        verify(listingBlackoutRepository, never())
                .saveAndFlush(any(ListingBlackout.class));
    }

    /**
     * createRejectsAnOverlapWithAnExistingBooking - This function
     * proves a blackout cannot be stored over an active booking.
     */
    @Test
    void createRejectsAnOverlapWithAnExistingBooking() {
        given(listingRepository.findByIdForUpdate(3L))
                .willReturn(Optional.of(listing));
        Booking reserved = TestBookings.withId(11L, listing, customer,
                START, END);
        given(bookingRepository.findUnavailableBookingsByListingId(3L))
                .willReturn(List.of(reserved));

        assertThatThrownBy(() -> blackoutService.create(ownerCaller, 3L,
                START, END, null))
                .isInstanceOf(BlackoutConflictException.class);

        verify(listingBlackoutRepository, never())
                .saveAndFlush(any(ListingBlackout.class));
    }

    /**
     * createRejectsAnOverlapWithAnExistingBlackout - This function
     * proves a second overlapping blackout on the same listing is
     * rejected before the insert is attempted.
     */
    @Test
    void createRejectsAnOverlapWithAnExistingBlackout() {
        given(listingRepository.findByIdForUpdate(3L))
                .willReturn(Optional.of(listing));
        given(bookingRepository.findUnavailableBookingsByListingId(3L))
                .willReturn(List.of());
        given(listingBlackoutRepository.existsOverlapping(3L, START, END))
                .willReturn(true);

        assertThatThrownBy(() -> blackoutService.create(ownerCaller, 3L,
                START, END, null))
                .isInstanceOf(BlackoutConflictException.class);

        verify(listingBlackoutRepository, never())
                .saveAndFlush(any(ListingBlackout.class));
    }

    /**
     * listForListingReturnsStoredBlackouts - This function proves
     * list is a plain passthrough with no auth gate.
     */
    @Test
    void listForListingReturnsStoredBlackouts() {
        ListingBlackout blackout = TestBlackouts.withId(5L, listing,
                START, END, "Vacation");
        given(listingBlackoutRepository.findByListingId(3L))
                .willReturn(List.of(blackout));

        assertThat(blackoutService.listForListing(3L))
                .containsExactly(blackout);
    }

    /**
     * deleteRemovesOwnedBlackout - This function proves a stored
     * blackout is removed when the caller owns its listing.
     */
    @Test
    void deleteRemovesOwnedBlackout() {
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(listing));
        ListingBlackout blackout = TestBlackouts.withId(5L, listing,
                START, END, null);
        given(listingBlackoutRepository.findById(5L))
                .willReturn(Optional.of(blackout));

        blackoutService.delete(ownerCaller, 3L, 5L);

        verify(listingBlackoutRepository).delete(blackout);
    }

    /**
     * deleteRejectsADifferentSupplier - This function proves another
     * SUPPLIER JWT cannot delete Ada's blackout.
     */
    @Test
    void deleteRejectsADifferentSupplier() {
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(listing));

        assertThatThrownBy(() -> blackoutService.delete(otherSupplier, 3L,
                5L))
                .isInstanceOf(ListingAccessDeniedException.class);

        verify(listingBlackoutRepository, never())
                .delete(any(ListingBlackout.class));
    }

    /**
     * deleteRejectsABlackoutFromADifferentListing - This function
     * proves a blackout id cannot be deleted through a listing it
     * does not belong to, even when the caller owns that listing.
     */
    @Test
    void deleteRejectsABlackoutFromADifferentListing() {
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(listing));
        Listing otherListing = TestListings.withId(4L, "Tent", "OUTDOOR",
                new BigDecimal("10.00"), owner);
        ListingBlackout blackout = TestBlackouts.withId(5L, otherListing,
                START, END, null);
        given(listingBlackoutRepository.findById(5L))
                .willReturn(Optional.of(blackout));

        assertThatThrownBy(() -> blackoutService.delete(ownerCaller, 3L,
                5L))
                .isInstanceOf(BlackoutNotFoundException.class);

        verify(listingBlackoutRepository, never())
                .delete(any(ListingBlackout.class));
    }

    /**
     * deleteRejectsAMissingBlackout - This function proves an
     * unknown blackout id is 404.
     */
    @Test
    void deleteRejectsAMissingBlackout() {
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(listing));
        given(listingBlackoutRepository.findById(99L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> blackoutService.delete(ownerCaller, 3L,
                99L))
                .isInstanceOf(BlackoutNotFoundException.class);
    }

    /**
     * givenNoConflicts - This function stubs the owned listing with
     * no existing booking or blackout overlap so the insert is the
     * next step.
     */
    private void givenNoConflicts() {
        given(listingRepository.findByIdForUpdate(3L))
                .willReturn(Optional.of(listing));
        given(bookingRepository.findUnavailableBookingsByListingId(3L))
                .willReturn(List.of());
        given(listingBlackoutRepository.existsOverlapping(3L, START, END))
                .willReturn(false);
    }
}
