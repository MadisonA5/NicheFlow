/**
 * This program is the black-box test suite for the booking module:
 * BK-01 create, BK-03 confirm, and BK-04 cancel. Every test goes
 * through the HTTP API with real JWTs against a real PostgreSQL
 * (Testcontainers) and checks only status codes and response fields.
 * Each test makes its own supplier, customers, and listing, so tests
 * never overlap each other's booking windows. Needs Docker.
 *
 * File:   BookingBlackBoxTest.java
 * Course: AUCSC 325
 */
package com.nicheflow;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BookingBlackBoxTest {

    private static final String PASSWORD = "correct-horse";
    private static final long MISSING_ID = 999_999_999L;

    private static final String T10 = "2031-03-01T10:00:00Z";
    private static final String T11 = "2031-03-01T11:00:00Z";
    private static final String T12 = "2031-03-01T12:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    // =================================================================
    // CREATE (BK-01): equivalence classes on who and what is booked
    // =================================================================

    /** A customer booking an active listing gets a PENDING booking. */
    @Test
    void customerCreatesAPendingBooking() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        String customer = login("CUSTOMER");

        book(customer, listingId, T10, T11)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.id").isNumber());
    }

    /** Only customers may book: a supplier is refused. */
    @Test
    void supplierCannotCreateABooking() throws Exception {
        String supplier = login("SUPPLIER");
        long listingId = createListing(supplier);

        book(supplier, listingId, T10, T11)
                .andExpect(status().isForbidden());
    }

    /** A listing id that does not exist is 404. */
    @Test
    void bookingAnUnknownListingIsNotFound() throws Exception {
        book(login("CUSTOMER"), MISSING_ID, T10, T11)
                .andExpect(status().isNotFound());
    }

    /** A deactivated listing cannot be booked (400). */
    @Test
    void bookingADeactivatedListingIsRejected() throws Exception {
        String supplier = login("SUPPLIER");
        long listingId = createListing(supplier);
        mockMvc.perform(post("/api/listings/" + listingId + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplier)))
                .andExpect(status().isOk());

        book(login("CUSTOMER"), listingId, T10, T11)
                .andExpect(status().isBadRequest());
    }

    // =================================================================
    // CREATE: boundary values on the time window and ids
    // =================================================================

    /** Boundary: end equal to start is not a forward window (400). */
    @Test
    void endEqualToStartIsRejected() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        book(login("CUSTOMER"), listingId, T10, T10)
                .andExpect(status().isBadRequest());
    }

    /** Boundary: end before start is rejected (400). */
    @Test
    void endBeforeStartIsRejected() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        book(login("CUSTOMER"), listingId, T11, T10)
                .andExpect(status().isBadRequest());
    }

    /** Boundary: the smallest legal window, one second long, works. */
    @Test
    void oneSecondWindowIsAccepted() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        book(login("CUSTOMER"), listingId, "2031-03-01T10:00:00Z",
                "2031-03-01T10:00:01Z")
                .andExpect(status().isCreated());
    }

    /** Boundary: listingId 0 breaks the minimum of 1 (400). */
    @Test
    void listingIdZeroIsRejected() throws Exception {
        book(login("CUSTOMER"), 0, T10, T11)
                .andExpect(status().isBadRequest());
    }

    /** A missing endTime fails validation (400). */
    @Test
    void missingEndTimeIsRejected() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(login("CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"listingId":%d,"startTime":"%s"}
                        """.formatted(listingId, T10)))
                .andExpect(status().isBadRequest());
    }

    // =================================================================
    // CREATE: overlapping windows on one listing
    // =================================================================

    /** Boundary: overlapping by one second is a conflict (409). */
    @Test
    void windowOverlappingByOneSecondIsAConflict() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        book(login("CUSTOMER"), listingId, T10, T11)
                .andExpect(status().isCreated());

        book(login("CUSTOMER"), listingId, "2031-03-01T10:59:59Z", T12)
                .andExpect(status().isConflict());
    }

    /**
     * Boundary: a window that starts exactly when the other ends is
     * allowed. Assumes the database range is half-open [start, end);
     */
    @Test
    void backToBackWindowsAreAllowed() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        book(login("CUSTOMER"), listingId, T10, T11)
                .andExpect(status().isCreated());

        book(login("CUSTOMER"), listingId, T11, T12)
                .andExpect(status().isCreated());
    }

    /** The same window on a different listing is not a conflict. */
    @Test
    void sameWindowOnADifferentListingIsAllowed() throws Exception {
        String supplier = login("SUPPLIER");
        long first = createListing(supplier);
        long second = createListing(supplier);
        book(login("CUSTOMER"), first, T10, T11)
                .andExpect(status().isCreated());

        book(login("CUSTOMER"), second, T10, T11)
                .andExpect(status().isCreated());
    }

    // =================================================================
    // CONFIRM (BK-03)
    // =================================================================

    /** The booking's own customer confirms: PENDING becomes CONFIRMED. */
    @Test
    void customerConfirmsTheirPendingBooking() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        String customer = login("CUSTOMER");
        long bookingId = createBooking(customer, listingId);

        changeStatus(customer, bookingId, "confirm")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    /** A different customer cannot confirm someone else's booking. */
    @Test
    void anotherCustomerCannotConfirm() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        long bookingId = createBooking(login("CUSTOMER"), listingId);

        changeStatus(login("CUSTOMER"), bookingId, "confirm")
                .andExpect(status().isForbidden());
    }

    /** The listing's supplier cannot confirm: only the customer can. */
    @Test
    void supplierCannotConfirm() throws Exception {
        String supplier = login("SUPPLIER");
        long listingId = createListing(supplier);
        long bookingId = createBooking(login("CUSTOMER"), listingId);

        changeStatus(supplier, bookingId, "confirm")
                .andExpect(status().isForbidden());
    }

    /** Confirming twice is an illegal move (CONFIRMED to CONFIRMED). */
    @Test
    void confirmingTwiceIsRejected() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        String customer = login("CUSTOMER");
        long bookingId = createBooking(customer, listingId);
        changeStatus(customer, bookingId, "confirm")
                .andExpect(status().isOk());

        changeStatus(customer, bookingId, "confirm")
                .andExpect(status().is4xxClientError());
    }

    /** A booking id that does not exist is 404. */
    @Test
    void confirmingAnUnknownBookingIsNotFound() throws Exception {
        changeStatus(login("CUSTOMER"), MISSING_ID, "confirm")
                .andExpect(status().isNotFound());
    }

    /** Boundary: booking id 0 breaks the minimum of 1 (400). */
    @Test
    void confirmingBookingIdZeroIsRejected() throws Exception {
        changeStatus(login("CUSTOMER"), 0, "confirm")
                .andExpect(status().isBadRequest());
    }

    // =================================================================
    // CANCEL (BK-04)
    // =================================================================

    /** The customer cancels a pending booking. */
    @Test
    void customerCancelsAPendingBooking() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        String customer = login("CUSTOMER");
        long bookingId = createBooking(customer, listingId);

        changeStatus(customer, bookingId, "cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /** The listing's supplier may cancel a confirmed booking. */
    @Test
    void supplierCancelsAConfirmedBooking() throws Exception {
        String supplier = login("SUPPLIER");
        long listingId = createListing(supplier);
        String customer = login("CUSTOMER");
        long bookingId = createBooking(customer, listingId);
        changeStatus(customer, bookingId, "confirm")
                .andExpect(status().isOk());

        changeStatus(supplier, bookingId, "cancel")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    /** A customer who is not on the booking cannot cancel it. */
    @Test
    void unrelatedCustomerCannotCancel() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        long bookingId = createBooking(login("CUSTOMER"), listingId);

        changeStatus(login("CUSTOMER"), bookingId, "cancel")
                .andExpect(status().isForbidden());
    }

    /** A supplier who does not own the listing cannot cancel. */
    @Test
    void otherSupplierCannotCancel() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        long bookingId = createBooking(login("CUSTOMER"), listingId);

        changeStatus(login("SUPPLIER"), bookingId, "cancel")
                .andExpect(status().isForbidden());
    }

    /** Cancelling an already cancelled booking is an illegal move. */
    @Test
    void cancellingTwiceIsRejected() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        String customer = login("CUSTOMER");
        long bookingId = createBooking(customer, listingId);
        changeStatus(customer, bookingId, "cancel")
                .andExpect(status().isOk());

        changeStatus(customer, bookingId, "cancel")
                .andExpect(status().is4xxClientError());
    }

    /** A cancelled booking cannot be confirmed afterwards. */
    @Test
    void cancelledBookingCannotBeConfirmed() throws Exception {
        long listingId = createListing(login("SUPPLIER"));
        String customer = login("CUSTOMER");
        long bookingId = createBooking(customer, listingId);
        changeStatus(customer, bookingId, "cancel")
                .andExpect(status().isOk());

        changeStatus(customer, bookingId, "confirm")
                .andExpect(status().is4xxClientError());
    }

    // =================================================================
    // HELPERS
    // =================================================================

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Registers a fresh user with the role and returns their JWT. */
    private String login(String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID()
                + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"%s"}
                        """.formatted(email, PASSWORD, role)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(),
                "$.token");
    }

    /** The supplier creates an active listing and we return its id. */
    private long createListing(String supplierToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Kayak %s","category":"SPORTS","price":50.00,\
                        "location":"Edmonton",\
                        "description":"A day on the river."}
                        """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(
                result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private ResultActions book(String token, long listingId, String start,
            String end) throws Exception {
        return mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"listingId":%d,"startTime":"%s","endTime":"%s"}
                        """.formatted(listingId, start, end)));
    }

    /** Creates a pending booking (10:00 to 11:00) and returns its id. */
    private long createBooking(String customerToken, long listingId)
            throws Exception {
        MvcResult result = book(customerToken, listingId, T10, T11)
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(
                result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    /** PUT /api/bookings/{id}/confirm or /cancel. */
    private ResultActions changeStatus(String token, long bookingId,
            String action) throws Exception {
        return mockMvc.perform(put("/api/bookings/" + bookingId + "/"
                + action)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }
}
