/**
 * This program is the one place that decides which JWT subject is
 * which party on a booking, shared by BookingService's BK-02
 * lifecycle and ReviewService's ADM-02 party resolution so the two
 * never drift on what "a party to this booking" means.
 *
 * File:   BookingParties.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.service;

import com.nicheflow.model.Booking;
import com.nicheflow.security.AuthenticatedUser;

final class BookingParties {

    /**
     * BookingParties - This function hides the helper constructor so
     * callers only use the static checks.
     */
    private BookingParties() {
    }

    /**
     * isCustomer - This function compares the JWT id to the customer
     * stored on the booking.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being checked
     *
     * Return - True when the caller is that customer
     */
    static boolean isCustomer(AuthenticatedUser caller, Booking booking) {
        return caller.id().equals(booking.getCustomer().getId());
    }

    /**
     * isSupplier - This function compares the JWT id to the supplier
     * who owns the booked listing.
     *
     * Parameters:
     * caller - The AuthenticatedUser from the security context
     * booking - The booking being checked
     *
     * Return - True when the caller owns that listing
     */
    static boolean isSupplier(AuthenticatedUser caller, Booking booking) {
        Long supplierId = booking.getListing().getSupplier().getId();
        return caller.id().equals(supplierId);
    }
}
