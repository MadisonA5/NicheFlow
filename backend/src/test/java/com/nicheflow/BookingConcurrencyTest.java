/**
 * This program proves BK-01 against a real PostgreSQL: two customers
 * who request the same window at once produce one pending booking,
 * and the other request fails with a conflict. It needs Docker.
 *
 * File:   BookingConcurrencyTest.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
 *     - Spring Boot Testcontainers service connections
 * https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-EXCLUSION
 *     - Exclusion constraints
 */
package com.nicheflow;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nicheflow.model.Booking;
import com.nicheflow.model.BookingStatus;
import com.nicheflow.repository.BookingRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BookingConcurrencyTest {

    private static final String PASSWORD = "correct-horse";
    private static final String WINDOW_START = "2026-11-01T15:00:00Z";
    private static final String WINDOW_END = "2026-11-01T16:00:00Z";
    private static final int CUSTOMER_COUNT = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    /**
     * overlappingConcurrentBookingsLetExactlyOneSucceed - This
     * function releases two customer creates for the same window at
     * the same moment. Exactly one insert commits. The other is a
     * conflict, and the table holds one PENDING row.
     */
    @Test
    void overlappingConcurrentBookingsLetExactlyOneSucceed()
            throws Exception {
        String supplierToken = loginAsNewUser("SUPPLIER");
        long listingId = createListing(supplierToken);
        String firstToken = loginAsNewUser("CUSTOMER");
        String secondToken = loginAsNewUser("CUSTOMER");
        String body = bookingBody(listingId, WINDOW_START, WINDOW_END);

        ExecutorService pool = Executors.newFixedThreadPool(
                CUSTOMER_COUNT);
        CountDownLatch ready = new CountDownLatch(CUSTOMER_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        try {
            // To arm both requests, then release them together
            Future<Integer> firstStatus = pool.submit(() ->
                    postBooking(firstToken, body, ready, start));
            Future<Integer> secondStatus = pool.submit(() ->
                    postBooking(secondToken, body, ready, start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = List.of(
                    firstStatus.get(30, TimeUnit.SECONDS),
                    secondStatus.get(30, TimeUnit.SECONDS));

            // To require one winner and one clean rejection
            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses.stream().filter(code -> code == 201)
                    .count()).isEqualTo(1);

            List<Booking> stored = bookingRepository.findByListingId(
                    listingId);
            assertThat(stored).hasSize(1);
            assertThat(stored.get(0).getStatus())
                    .isEqualTo(BookingStatus.PENDING);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * aLaterOverlappingBookingIsRejected - This function books a
     * window, then sends a second request that overlaps it, and
     * expects the conflict after the first row has committed.
     */
    @Test
    void aLaterOverlappingBookingIsRejected() throws Exception {
        String supplierToken = loginAsNewUser("SUPPLIER");
        long listingId = createListing(supplierToken);
        String firstToken = loginAsNewUser("CUSTOMER");
        String secondToken = loginAsNewUser("CUSTOMER");

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listingId, WINDOW_START,
                        WINDOW_END)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(secondToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listingId, "2026-11-01T15:30:00Z",
                        "2026-11-01T16:30:00Z")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Booking conflict"));

        assertThat(bookingRepository.findByListingId(listingId))
                .hasSize(1);
    }

    /**
     * aBookingThatStartsWhenAnotherEndsSucceeds - This function
     * books two touching windows. The range is half-open, so the
     * second customer is not an overlap.
     */
    @Test
    void aBookingThatStartsWhenAnotherEndsSucceeds() throws Exception {
        String supplierToken = loginAsNewUser("SUPPLIER");
        long listingId = createListing(supplierToken);
        String firstToken = loginAsNewUser("CUSTOMER");
        String secondToken = loginAsNewUser("CUSTOMER");

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(firstToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listingId, WINDOW_START,
                        WINDOW_END)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(secondToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookingBody(listingId, WINDOW_END,
                        "2026-11-01T17:00:00Z")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(bookingRepository.findByListingId(listingId))
                .hasSize(2);
    }

    /**
     * postBooking - This function waits until both callers are ready,
     * then POSTs one booking and returns the HTTP status.
     *
     * Parameters:
     * token - The customer JWT
     * body - The booking JSON
     * ready - The latch this caller counts down before waiting
     * start - The latch that releases both callers together
     *
     * Return - The HTTP status from the create call
     */
    private int postBooking(String token, String body,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        boolean released = start.await(10, TimeUnit.SECONDS);
        if (!released) {
            return -1;
        }

        MvcResult result = mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andReturn();

        return result.getResponse().getStatus();
    }

    /**
     * loginAsNewUser - This function registers a unique account with
     * the given role and logs in so each test has its own JWT.
     *
     * Parameters:
     * role - CUSTOMER or SUPPLIER
     *
     * Return - The Bearer token from login
     */
    private String loginAsNewUser(String role) throws Exception {
        String email = "bk-" + UUID.randomUUID() + "@example.com";

        // To persist the account through AUTH-01
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"%s"}
                        """.formatted(email, PASSWORD, role)))
                .andExpect(status().isCreated());

        // To exchange those credentials for a JWT
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(loginResult.getResponse().getContentAsString(),
                "$.token");
    }

    /**
     * createListing - This function POSTs a listing and returns the
     * generated id so a booking can name it.
     *
     * Parameters:
     * token - The supplier JWT
     *
     * Return - The persisted listing id
     */
    private long createListing(String token) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Kayak","category":"SPORTS","price":25.00}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        Number listingId = JsonPath.read(
                result.getResponse().getContentAsString(), "$.id");

        return listingId.longValue();
    }

    /**
     * bookingBody - This function builds the create JSON body.
     *
     * Parameters:
     * listingId - The listing to book
     * startTime - The slot start as an ISO-8601 UTC string
     * endTime - The slot end as an ISO-8601 UTC string
     *
     * Return - The JSON string
     */
    private static String bookingBody(long listingId, String startTime,
            String endTime) {
        return """
                {"listingId":%d,"startTime":"%s","endTime":"%s"}
                """.formatted(listingId, startTime, endTime);
    }

    /**
     * bearer - This function prefixes a JWT with the Bearer scheme
     * the filter expects.
     *
     * Parameters:
     * token - The compact JWT from login
     *
     * Return - The Authorization header value
     */
    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
