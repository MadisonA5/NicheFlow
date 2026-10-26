package com.nicheflow.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.nicheflow.dto.AccountBookingResponse;
import com.nicheflow.dto.AccountResponse;
import com.nicheflow.dto.ListingResponse;
import com.nicheflow.model.Role;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.security.AuthenticatedUser;

/**
 * Returns account activity scoped to the identity in the verified JWT.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;

    public AccountController(ListingRepository listingRepository,
            BookingRepository bookingRepository) {
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * Loads the caller's listings and/or bookings based on their role.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public AccountResponse account(
            @AuthenticationPrincipal AuthenticatedUser caller) {
        if (caller.role() == Role.SUPPLIER) {
            List<ListingResponse> listings = listingRepository
                    .findAllBySupplierId(caller.id()).stream()
                    .map(ListingResponse::from)
                    .toList();
            List<AccountBookingResponse> bookings = bookingRepository
                    .findAllForSupplier(caller.id()).stream()
                    .map(AccountBookingResponse::from)
                    .toList();
            return new AccountResponse(listings, bookings);
        }

        if (caller.role() == Role.CUSTOMER) {
            List<AccountBookingResponse> bookings = bookingRepository
                    .findAllForCustomer(caller.id()).stream()
                    .map(AccountBookingResponse::from)
                    .toList();
            return new AccountResponse(List.of(), bookings);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Account data is not available for this role.");
    }
}
