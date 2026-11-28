package com.nicheflow.controller;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.nicheflow.model.Booking;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.ReviewRepository.ListingRatingSummary;
import com.nicheflow.repository.ReviewRepository.UserRatingSummary;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.ListingService;
import com.nicheflow.service.ReviewService;

/**
 * Returns account activity scoped to the identity in the verified JWT.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final ListingService listingService;
    private final ReviewService reviewService;

    public AccountController(ListingRepository listingRepository,
            BookingRepository bookingRepository,
            ListingService listingService, ReviewService reviewService) {
        this.listingRepository = listingRepository;
        this.bookingRepository = bookingRepository;
        this.listingService = listingService;
        this.reviewService = reviewService;
    }

    /**
     * Loads the caller's listings and/or bookings based on their role.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public AccountResponse account(
            @AuthenticationPrincipal AuthenticatedUser caller) {
        if (caller.role() == Role.SUPPLIER) {
            List<Listing> supplierListings = listingRepository
                    .findAllBySupplierId(caller.id());

            // To match the ADM-02 rating every other listing endpoint
            // reports, not reset it to null/0 on the account page
            List<Long> listingIds = supplierListings.stream()
                    .map(Listing::getId)
                    .toList();
            Map<Long, ListingRatingSummary> listingRatings =
                    listingService.ratingSummaries(listingIds);
            List<ListingResponse> listings = supplierListings.stream()
                    .map(listing -> ListingResponse.from(listing,
                            listingRatings))
                    .toList();

            List<Booking> supplierBookings = bookingRepository
                    .findAllForSupplier(caller.id());

            // To let the supplier judge a request, including a still
            // PENDING one, by the customer's ADM-02 standing with
            // other suppliers, excluding the caller's own past
            // review of that same customer
            List<Long> customerIds = supplierBookings.stream()
                    .map(booking -> booking.getCustomer().getId())
                    .distinct()
                    .toList();
            Map<Long, UserRatingSummary> customerRatings = reviewService
                    .customerRatingSummaries(customerIds, caller.id());

            // To tell the account page which completed bookings the
            // caller has already reviewed in one query, instead of
            // one request per booking
            List<Long> bookingIds = supplierBookings.stream()
                    .map(Booking::getId)
                    .toList();
            Set<Long> reviewedBookingIds = reviewService
                    .reviewedBookingIds(bookingIds, caller.id());

            List<AccountBookingResponse> bookings = supplierBookings
                    .stream()
                    .map(booking -> {
                        UserRatingSummary summary = customerRatings
                                .get(booking.getCustomer().getId());
                        boolean reviewed = reviewedBookingIds
                                .contains(booking.getId());
                        return summary == null
                                ? AccountBookingResponse.from(booking,
                                        reviewed)
                                : AccountBookingResponse.from(booking,
                                        summary.getAverageRating(),
                                        summary.getReviewCount(),
                                        reviewed);
                    })
                    .toList();

            return new AccountResponse(listings, bookings);
        }

        if (caller.role() == Role.CUSTOMER) {
            List<Booking> customerBookings = bookingRepository
                    .findAllForCustomer(caller.id());

            List<Long> bookingIds = customerBookings.stream()
                    .map(Booking::getId)
                    .toList();
            Set<Long> reviewedBookingIds = reviewService
                    .reviewedBookingIds(bookingIds, caller.id());

            List<AccountBookingResponse> bookings = customerBookings
                    .stream()
                    .map(booking -> AccountBookingResponse.from(booking,
                            reviewedBookingIds.contains(booking.getId())))
                    .toList();
            return new AccountResponse(List.of(), bookings);
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Account data is not available for this role.");
    }
}
