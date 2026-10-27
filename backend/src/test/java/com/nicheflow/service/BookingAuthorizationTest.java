/**
 * This program proves who may move a booking, and that BK-03 confirm
 * charges once. The customer confirms. The customer, the listing
 * supplier, or an admin cancels. The supplier completes. Anyone else
 * is refused. A second confirm reuses the idempotency key.
 *
 * File:   BookingAuthorizationTest.java
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nicheflow.TestBookings;
import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.dto.BookingAvailabilityResponse;
import com.nicheflow.exception.BookingAccessDeniedException;
import com.nicheflow.model.Booking;
import com.nicheflow.model.BookingStatus;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.payment.MockStripePaymentClient;
import com.nicheflow.payment.PaymentCharge;
import com.nicheflow.payment.PaymentClient;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class BookingAuthorizationTest {

    private static final Instant START = Instant.parse(
            "2026-11-01T15:00:00Z");
    private static final Instant END = Instant.parse(
            "2026-11-01T16:00:00Z");
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
    private final AuthenticatedUser otherSupplierCaller =
            new AuthenticatedUser(6L, "other-supplier@example.com",
                    Role.SUPPLIER);

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

    @Test
    void getUnavailablePeriodsReturnsOnlyBookingTimeWindows() {
        Booking pending = TestBookings.withId(BOOKING_ID, listing, customer,
                START, END);
        given(bookingRepository.findUnavailableBookingsByListingId(3L))
                .willReturn(List.of(pending));

        assertThat(bookingService.getUnavailablePeriods(3L))
                .containsExactly(new BookingAvailabilityResponse(START, END));
    }

    /**
     * customerCanConfirmTheirOwnPendingBooking - This function stores
     * pending to confirmed for the booking's own customer.
     */
    @Test
    void customerCanConfirmTheirOwnPendingBooking() {
        Booking booking = load(BookingStatus.PENDING);
        stubSave();

        Booking stored = bookingService.transition(customerCaller,
                BOOKING_ID, BookingStatus.CONFIRMED);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(bookingRepository).save(booking);
    }

    @Test
    void supplierCanAcceptTheirOwnPendingBooking() {
        Booking booking = load(BookingStatus.PENDING);
        stubSave();

        Booking stored = bookingService.accept(supplierCaller, BOOKING_ID);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.ACCEPTED);
        verify(bookingRepository).save(booking);
        verify(paymentClient, never()).charge(any(), any());
    }

    @Test
    void supplierCannotAcceptAnotherSuppliersBooking() {
        assertDenied(otherSupplierCaller, BookingStatus.PENDING,
                BookingStatus.ACCEPTED);
    }

    @Test
    void customerCannotAcceptABooking() {
        assertDenied(customerCaller, BookingStatus.PENDING,
                BookingStatus.ACCEPTED);
    }

    /**
     * customerCanCancelTheirOwnPendingBooking - This function stores
     * pending to cancelled for the booking's own customer.
     */
    @Test
    void customerCanCancelTheirOwnPendingBooking() {
        Booking booking = load(BookingStatus.PENDING);
        stubSave();

        Booking stored = bookingService.transition(customerCaller,
                BOOKING_ID, BookingStatus.CANCELLED);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    /**
     * supplierCanCancelAConfirmedBooking - This function stores
     * confirmed to cancelled for the listing's supplier.
     */
    @Test
    void supplierCanCancelAConfirmedBooking() {
        Booking booking = load(BookingStatus.CONFIRMED);
        stubSave();

        Booking stored = bookingService.transition(supplierCaller,
                BOOKING_ID, BookingStatus.CANCELLED);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    /**
     * adminCanCancelAPendingBooking - This function stores pending to
     * cancelled for an admin who does not own the booking.
     */
    @Test
    void adminCanCancelAPendingBooking() {
        Booking booking = load(BookingStatus.PENDING);
        stubSave();

        Booking stored = bookingService.transition(adminCaller,
                BOOKING_ID, BookingStatus.CANCELLED);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookingRepository).save(booking);
    }

    /**
     * supplierCanCompleteAConfirmedBooking - This function stores
     * confirmed to completed for the listing's supplier.
     */
    @Test
    void supplierCanCompleteAConfirmedBooking() {
        Booking booking = load(BookingStatus.CONFIRMED);
        stubSave();

        Booking stored = bookingService.transition(supplierCaller,
                BOOKING_ID, BookingStatus.COMPLETED);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.COMPLETED);
        verify(bookingRepository).save(booking);
    }

    /**
     * supplierCannotConfirm - This function refuses a listing supplier
     * who tries to confirm the customer's booking.
     */
    @Test
    void supplierCannotConfirm() {
        assertDenied(supplierCaller, BookingStatus.PENDING,
                BookingStatus.CONFIRMED);
    }

    /**
     * adminCannotConfirm - This function refuses an admin who tries
     * to confirm a pending booking.
     */
    @Test
    void adminCannotConfirm() {
        assertDenied(adminCaller, BookingStatus.PENDING,
                BookingStatus.CONFIRMED);
    }

    /**
     * otherCustomerCannotConfirm - This function refuses a customer
     * who does not own the booking.
     */
    @Test
    void otherCustomerCannotConfirm() {
        assertDenied(otherCustomerCaller, BookingStatus.PENDING,
                BookingStatus.CONFIRMED);
    }

    /**
     * otherCustomerCannotCancel - This function refuses a customer
     * who is not the booker, the supplier, or an admin.
     */
    @Test
    void otherCustomerCannotCancel() {
        assertDenied(otherCustomerCaller, BookingStatus.PENDING,
                BookingStatus.CANCELLED);
    }

    /**
     * customerCannotComplete - This function refuses the booking
     * customer who tries to mark the booking completed.
     */
    @Test
    void customerCannotComplete() {
        assertDenied(customerCaller, BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED);
    }

    /**
     * adminCannotComplete - This function refuses an admin who tries
     * to mark the booking completed.
     */
    @Test
    void adminCannotComplete() {
        assertDenied(adminCaller, BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED);
    }

    /**
     * otherSupplierCannotComplete - This function refuses a supplier
     * who does not own the listing.
     */
    @Test
    void otherSupplierCannotComplete() {
        assertDenied(otherSupplierCaller, BookingStatus.CONFIRMED,
                BookingStatus.COMPLETED);
    }

    /**
     * confirmChargesBeforeTheStatusChanges - This function proves the
     * mock charge runs while the booking is still pending, and the
     * saved row is confirmed afterward.
     */
    @Test
    void confirmChargesBeforeTheStatusChanges() {
        Booking booking = load(BookingStatus.PENDING);
        given(paymentClient.charge(eq(PRICE),
                eq(BookingService.confirmIdempotencyKey(BOOKING_ID))))
                .willAnswer(call -> {
                    assertThat(booking.getStatus())
                            .isEqualTo(BookingStatus.PENDING);
                    return new PaymentCharge(
                            BookingService.confirmIdempotencyKey(
                                    BOOKING_ID),
                            PRICE);
                });
        stubSave();

        Booking stored = bookingService.confirm(customerCaller,
                BOOKING_ID);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(paymentClient).charge(PRICE,
                BookingService.confirmIdempotencyKey(BOOKING_ID));
    }

    @Test
    void customerCanConfirmAndPayAfterSupplierAccepts() {
        Booking booking = load(BookingStatus.ACCEPTED);
        given(paymentClient.charge(eq(PRICE),
                eq(BookingService.confirmIdempotencyKey(BOOKING_ID))))
                .willReturn(new PaymentCharge(
                        BookingService.confirmIdempotencyKey(BOOKING_ID),
                        PRICE));
        stubSave();

        Booking stored = bookingService.confirm(customerCaller, BOOKING_ID);

        assertThat(stored.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        verify(paymentClient).charge(PRICE,
                BookingService.confirmIdempotencyKey(BOOKING_ID));
    }

    /**
     * retriedConfirmDoesNotDoubleCharge - This function confirms,
     * puts the row back to pending as if the status write was lost,
     * and confirms again. The mock Stripe client keeps one charge.
     */
    @Test
    void retriedConfirmDoesNotDoubleCharge() {
        MockStripePaymentClient payments = new MockStripePaymentClient();
        BookingService service = new BookingService(bookingRepository,
                listingRepository, userRepository, payments);
        Booking booking = load(BookingStatus.PENDING);
        stubSave();

        service.confirm(customerCaller, BOOKING_ID);
        assertThat(payments.chargeCount()).isEqualTo(1);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        // To retry the confirm after the charge survived and the
        // status write did not
        booking.setStatus(BookingStatus.PENDING);
        service.confirm(customerCaller, BOOKING_ID);

        assertThat(payments.chargeCount()).isEqualTo(1);
        assertThat(payments.chargeFor(
                BookingService.confirmIdempotencyKey(BOOKING_ID))
                .amount()).isEqualByComparingTo(PRICE);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    /**
     * assertDenied - This function expects an access denial, an
     * unchanged status, and no save.
     *
     * Parameters:
     * caller - The party who must be refused
     * current - The status the booking starts in
     * next - The status the caller asked for
     */
    private void assertDenied(AuthenticatedUser caller,
            BookingStatus current, BookingStatus next) {
        Booking booking = load(current);

        assertThatThrownBy(() -> bookingService.transition(caller,
                BOOKING_ID, next))
                .isInstanceOf(BookingAccessDeniedException.class)
                .hasMessage(BookingService.CHANGE_DENIED);

        assertThat(booking.getStatus()).isEqualTo(current);
        verify(bookingRepository, never()).save(any());
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

    /**
     * stubSave - This function makes save return the booking it was
     * given, so a successful move can be read back.
     */
    private void stubSave() {
        given(bookingRepository.save(any(Booking.class)))
                .willAnswer(call -> call.getArgument(0));
    }
}
