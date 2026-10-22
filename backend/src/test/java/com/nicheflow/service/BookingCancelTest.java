/**
 * This program proves BK-04 cancel. The customer, the listing
 * supplier, and an admin each store a refund that matches the
 * timing. A non-party is refused. A completed booking cannot be
 * cancelled.
 *
 * File:   BookingCancelTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.exception.BookingAccessDeniedException;
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
class BookingCancelTest {

    private static final Instant END = Instant.parse(
            "2026-12-01T16:00:00Z");
    private static final long BOOKING_ID = 11L;
    private static final BigDecimal PRICE = new BigDecimal("25.00");

    private final User customer = TestUsers.withId(9L, "bob@example.com",
            "$2a$10$hashed", Role.CUSTOMER);
    private final User supplier = TestUsers.withId(7L, "ada@example.com",
            "$2a$10$hashed", Role.SUPPLIER);
    private final Listing listing = TestListings.withId(3L, "Kayak",
            "SPORTS", PRICE, supplier);
    private final AuthenticatedUser customerCaller = new AuthenticatedUser(
            9L, "bob@example.com", Role.CUSTOMER);
    private final AuthenticatedUser supplierCaller = new AuthenticatedUser(
            7L, "ada@example.com", Role.SUPPLIER);
    private final AuthenticatedUser adminCaller = new AuthenticatedUser(
            4L, "admin@example.com", Role.ADMIN);
    private final AuthenticatedUser otherCustomerCaller =
            new AuthenticatedUser(8L, "other@example.com", Role.CUSTOMER);

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
     * customerCancelMoreThan24HoursAheadStoresTheFullPrice - This
     * function cancels a pending booking whose start is 48 hours
     * away and stores the full listing price.
     */
    @Test
    void customerCancelMoreThan24HoursAheadStoresTheFullPrice() {
        Instant startTime = Instant.now().plus(48, ChronoUnit.HOURS);
        Booking booking = load(BookingStatus.PENDING, startTime, PRICE);
        stubSave();

        Booking stored = bookingService.cancel(customerCaller, BOOKING_ID);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(stored.getRefundAmount()).isEqualByComparingTo(PRICE);
        verify(bookingRepository).save(booking);
        verify(paymentClient, never()).charge(any(), any());
    }

    /**
     * supplierCancelInside24HoursStoresHalfThePrice - This function
     * cancels a confirmed booking whose start is 2 hours away.
     * 10.01 times one half rounds half up to 5.01.
     */
    @Test
    void supplierCancelInside24HoursStoresHalfThePrice() {
        Instant startTime = Instant.now().plus(2, ChronoUnit.HOURS);
        BigDecimal price = new BigDecimal("10.01");
        Booking booking = load(BookingStatus.CONFIRMED, startTime, price);
        stubSave();

        Booking stored = bookingService.cancel(supplierCaller, BOOKING_ID);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(stored.getRefundAmount()).isEqualByComparingTo("5.01");
        verify(bookingRepository).save(booking);
    }

    /**
     * adminCancelAfterTheStartStoresZero - This function cancels a
     * pending booking whose start has already passed and stores 0.00.
     */
    @Test
    void adminCancelAfterTheStartStoresZero() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Booking booking = load(BookingStatus.PENDING, startTime, PRICE);
        stubSave();

        Booking stored = bookingService.cancel(adminCaller, BOOKING_ID);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(stored.getRefundAmount()).isEqualByComparingTo("0.00");
        verify(bookingRepository).save(booking);
    }

    /**
     * otherCustomerCannotCancel - This function refuses a customer
     * who is not a party, and does not save.
     */
    @Test
    void otherCustomerCannotCancel() {
        Instant startTime = Instant.now().plus(48, ChronoUnit.HOURS);
        Booking booking = load(BookingStatus.PENDING, startTime, PRICE);

        assertThatThrownBy(() -> bookingService.cancel(otherCustomerCaller,
                BOOKING_ID))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessage(BookingService.CHANGE_DENIED);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(booking.getRefundAmount()).isNull();
        verify(bookingRepository, never()).save(any());
    }

    /**
     * completedBookingCannotBeCancelled - This function refuses a
     * move out of completed and does not save.
     */
    @Test
    void completedBookingCannotBeCancelled() {
        Instant startTime = Instant.now().plus(48, ChronoUnit.HOURS);
        Booking booking = load(BookingStatus.COMPLETED, startTime, PRICE);

        assertThatThrownBy(() -> bookingService.cancel(customerCaller,
                BOOKING_ID))
                .isInstanceOf(InvalidBookingTransitionException.class)
                .hasMessage("Cannot move a booking from COMPLETED to "
                        + "CANCELLED");

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        assertThat(booking.getRefundAmount()).isNull();
        verify(bookingRepository, never()).save(any());
    }

    /**
     * load - This function stubs a booking with the given status,
     * start, and listing price.
     *
     * Parameters:
     * status - The lifecycle state to stamp on the booking
     * startTime - The UTC start used by the refund rule
     * price - The listing price to refund against
     *
     * Return - The booking with id 11
     */
    private Booking load(BookingStatus status, Instant startTime,
            BigDecimal price) {
        listing.setPrice(price);
        Booking booking = TestBookings.withId(BOOKING_ID, listing,
                customer, startTime, END);
        booking.setStatus(status);
        given(bookingRepository.findById(BOOKING_ID))
                .willReturn(Optional.of(booking));
        return booking;
    }

    /**
     * stubSave - This function makes save return the booking it was
     * given, so the refund can be read back.
     */
    private void stubSave() {
        given(bookingRepository.save(any(Booking.class)))
                .willAnswer(call -> call.getArgument(0));
    }
}
