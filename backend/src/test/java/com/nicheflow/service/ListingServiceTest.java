/**
 * This program proves LST-01 create, edit, and deactivate in the
 * service: the JWT AuthenticatedUser is the owner, a second supplier
 * is 403, and deactivate never deletes the row.
 *
 * File:   ListingServiceTest.java
 * Course: AUCSC 325
 */
package com.nicheflow.service;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nicheflow.TestListings;
import com.nicheflow.TestUsers;
import com.nicheflow.exception.ListingAccessDeniedException;
import com.nicheflow.exception.ListingNotFoundException;
import com.nicheflow.model.Listing;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.security.AuthenticatedUser;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    private static final BigDecimal PRICE = new BigDecimal("25.00");

    private final User owner = TestUsers.withId(7L, "ada@example.com",
            "$2a$10$hashed", Role.SUPPLIER);
    private final AuthenticatedUser ownerCaller = new AuthenticatedUser(
            7L, "ada@example.com", Role.SUPPLIER);
    private final AuthenticatedUser otherSupplier = new AuthenticatedUser(
            8L, "other@example.com", Role.SUPPLIER);
    private final AuthenticatedUser customerCaller = new AuthenticatedUser(
            9L, "bob@example.com", Role.CUSTOMER);

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingService listingService;

    @Captor
    private ArgumentCaptor<Listing> savedListing;

    /**
     * createStoresTheJwtCallerAsTheSupplier - This function checks
     * that the saved listing's owner is the User loaded by the JWT
     * id, not a client-chosen supplier.
     */
    @Test
    void createStoresTheJwtCallerAsTheSupplier() {
        given(userRepository.findById(7L)).willReturn(Optional.of(owner));
        given(listingRepository.save(any(Listing.class)))
                .willAnswer(call -> call.getArgument(0));

        listingService.create(ownerCaller, "Kayak", "SPORTS", PRICE);

        verify(listingRepository).save(savedListing.capture());
        assertThat(savedListing.getValue().getSupplier()).isSameAs(owner);
        assertThat(savedListing.getValue().isActive()).isTrue();
    }

    /**
     * createRejectsACustomerCaller - This function proves a CUSTOMER
     * JWT cannot open a listing.
     */
    @Test
    void createRejectsACustomerCaller() {
        assertThatThrownBy(() -> listingService.create(customerCaller,
                "Kayak", "SPORTS", PRICE))
                .isInstanceOf(ListingAccessDeniedException.class);

        verify(listingRepository, never()).save(any(Listing.class));
    }

    /**
     * updateChangesFieldsWhenTheJwtCallerOwnsTheListing - This
     * function edits title, category, and price for the owner.
     */
    @Test
    void updateChangesFieldsWhenTheJwtCallerOwnsTheListing() {
        Listing stored = TestListings.withId(3L, "Old", "OLD",
                new BigDecimal("10.00"), owner);
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(stored));
        given(listingRepository.save(any(Listing.class)))
                .willAnswer(call -> call.getArgument(0));

        Listing updated = listingService.update(ownerCaller, 3L,
                "New kayak", "WATER", PRICE);

        assertThat(updated.getTitle()).isEqualTo("New kayak");
        assertThat(updated.getCategory()).isEqualTo("WATER");
        assertThat(updated.getPrice()).isEqualByComparingTo(PRICE);
        assertThat(updated.getSupplier()).isSameAs(owner);
    }

    /**
     * updateRejectsADifferentSupplier - This function proves another
     * SUPPLIER JWT cannot edit Ada's listing.
     */
    @Test
    void updateRejectsADifferentSupplier() {
        Listing stored = TestListings.withId(3L, "Kayak", "SPORTS",
                PRICE, owner);
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(stored));

        assertThatThrownBy(() -> listingService.update(otherSupplier, 3L,
                "Stolen", "SPORTS", PRICE))
                .isInstanceOf(ListingAccessDeniedException.class);

        verify(listingRepository, never()).save(any(Listing.class));
    }

    /**
     * deactivateSetsActiveFalseAndDoesNotDelete - This function hides
     * the listing and never calls delete.
     */
    @Test
    void deactivateSetsActiveFalseAndDoesNotDelete() {
        Listing stored = TestListings.withId(3L, "Kayak", "SPORTS",
                PRICE, owner);
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(stored));
        given(listingRepository.save(any(Listing.class)))
                .willAnswer(call -> call.getArgument(0));

        Listing hidden = listingService.deactivate(ownerCaller, 3L);

        assertThat(hidden.isActive()).isFalse();
        verify(listingRepository).save(stored);
        verify(listingRepository, never()).delete(any(Listing.class));
        verify(listingRepository, never()).deleteById(any());
    }

    /**
     * deactivateRejectsADifferentSupplier - This function proves
     * another SUPPLIER JWT cannot hide Ada's listing.
     */
    @Test
    void deactivateRejectsADifferentSupplier() {
        Listing stored = TestListings.withId(3L, "Kayak", "SPORTS",
                PRICE, owner);
        given(listingRepository.findById(3L))
                .willReturn(Optional.of(stored));

        assertThatThrownBy(() -> listingService.deactivate(otherSupplier,
                3L))
                .isInstanceOf(ListingAccessDeniedException.class);

        verify(listingRepository, never()).save(any(Listing.class));
        verify(listingRepository, never()).deleteById(any());
    }

    /**
     * updateRejectsAMissingListing - This function proves an unknown
     * id is 404, not a silent create.
     */
    @Test
    void updateRejectsAMissingListing() {
        given(listingRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> listingService.update(ownerCaller, 99L,
                "Kayak", "SPORTS", PRICE))
                .isInstanceOf(ListingNotFoundException.class);
    }
}
