/**
 * This program proves ADM-02 against a real PostgreSQL: two review
 * creates from the same reviewer on the same booking at once produce
 * one stored review, and the other request fails with a conflict,
 * while each party may still leave their own independent review. It
 * needs Docker.
 *
 * File:   ReviewConcurrencyTest.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
 *     - Spring Boot Testcontainers service connections
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nicheflow.repository.ReviewRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ReviewConcurrencyTest {

    private static final String PASSWORD = "correct-horse";
    private static final int ATTEMPT_COUNT = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    /**
     * twoSimultaneousReviewsFromTheSameReviewerLetExactlyOneSucceed -
     * This function releases two review creates from the same
     * customer on the same completed booking at the same moment.
     * Exactly one insert commits. The other is a conflict, and the
     * table holds one row for that booking.
     */
    @Test
    void twoSimultaneousReviewsFromTheSameReviewerLetExactlyOneSucceed()
            throws Exception {
        String customerToken = loginAsNewUser("CUSTOMER");
        String supplierToken = loginAsNewUser("SUPPLIER");
        long bookingId = completeBooking(customerToken, supplierToken);
        String body = reviewBody(5, "Great stay");

        ExecutorService pool = Executors.newFixedThreadPool(
                ATTEMPT_COUNT);
        CountDownLatch ready = new CountDownLatch(ATTEMPT_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        try {
            // To arm both requests, then release them together
            Future<Integer> firstStatus = pool.submit(() ->
                    postReview(customerToken, bookingId, body, ready,
                            start));
            Future<Integer> secondStatus = pool.submit(() ->
                    postReview(customerToken, bookingId, body, ready,
                            start));

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = List.of(
                    firstStatus.get(30, TimeUnit.SECONDS),
                    secondStatus.get(30, TimeUnit.SECONDS));

            // To require one winner and one clean rejection
            assertThat(statuses).containsOnly(201, 409);
            assertThat(statuses.stream().filter(code -> code == 201)
                    .count()).isEqualTo(1);

            assertThat(reviewRepository.findByBookingId(bookingId))
                    .hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * aSecondReviewFromTheSameReviewerAfterTheFirstCommitsIsRejected -
     * This function stores a review, then sends a second one from the
     * same reviewer on the same booking, and expects the conflict
     * after the first row has committed.
     */
    @Test
    void aSecondReviewFromTheSameReviewerAfterTheFirstCommitsIsRejected()
            throws Exception {
        String customerToken = loginAsNewUser("CUSTOMER");
        String supplierToken = loginAsNewUser("SUPPLIER");
        long bookingId = completeBooking(customerToken, supplierToken);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(5, "Great stay")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings/" + bookingId + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(4, "Second attempt")))
                .andExpect(status().isConflict());

        assertThat(reviewRepository.findByBookingId(bookingId))
                .hasSize(1);
    }

    /**
     * bothPartiesCanReviewTheSameBookingIndependently - This function
     * proves the unique constraint is scoped per reviewer, not per
     * booking, so the customer and the supplier can each leave their
     * own review of the same booking.
     */
    @Test
    void bothPartiesCanReviewTheSameBookingIndependently()
            throws Exception {
        String customerToken = loginAsNewUser("CUSTOMER");
        String supplierToken = loginAsNewUser("SUPPLIER");
        long bookingId = completeBooking(customerToken, supplierToken);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(5, "Great stay")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings/" + bookingId + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(4, "Easy customer")))
                .andExpect(status().isCreated());

        assertThat(reviewRepository.findByBookingId(bookingId))
                .hasSize(2);
    }

    /**
     * aSuppliersReviewOfTheCustomerDoesNotCountTowardTheListing -
     * This function proves a supplier cannot pad their own listing's
     * public reputation by reviewing their customer: only the
     * customer's review of the supplier shows up on the listing's
     * review list and rating average.
     */
    @Test
    void aSuppliersReviewOfTheCustomerDoesNotCountTowardTheListing()
            throws Exception {
        String customerToken = loginAsNewUser("CUSTOMER");
        String supplierToken = loginAsNewUser("SUPPLIER");
        long listingId = createListing(supplierToken);
        long bookingId = completeBooking(customerToken, supplierToken,
                listingId);

        mockMvc.perform(post("/api/bookings/" + bookingId + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(5, "Easy customer")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/listings/" + listingId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/bookings/" + bookingId + "/reviews")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(4, "Great stay")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/listings/" + listingId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].rating").value(4));

        mockMvc.perform(get("/api/listings/" + listingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
    }

    /**
     * postReview - This function waits until both callers are ready,
     * then POSTs one review and returns the HTTP status.
     *
     * Parameters:
     * token - The reviewer's JWT
     * bookingId - The booking being reviewed
     * body - The review JSON
     * ready - The latch this caller counts down before waiting
     * start - The latch that releases both callers together
     *
     * Return - The HTTP status from the create call
     */
    private int postReview(String token, long bookingId, String body,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        boolean released = start.await(10, TimeUnit.SECONDS);
        if (!released) {
            return -1;
        }

        MvcResult result = mockMvc.perform(
                post("/api/bookings/" + bookingId + "/reviews")
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
        String email = "adm02-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"%s"}
                        """.formatted(email, PASSWORD, role)))
                .andExpect(status().isCreated());

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
     * completeBooking - This function runs a booking through create,
     * accept, confirm, and complete so it reaches the only status
     * ADM-02 allows a review on.
     *
     * Parameters:
     * customerToken - The customer JWT
     * supplierToken - The listing supplier's JWT
     *
     * Return - The id of the now-completed booking
     */
    private long completeBooking(String customerToken,
            String supplierToken) throws Exception {
        return completeBooking(customerToken, supplierToken,
                createListing(supplierToken));
    }

    /**
     * completeBooking - This function is completeBooking, but against
     * a listing the caller already created, so a test can query that
     * listing's reviews or rating afterward.
     *
     * Parameters:
     * customerToken - The customer JWT
     * supplierToken - The listing supplier's JWT
     * listingId - The listing to book
     *
     * Return - The id of the now-completed booking
     */
    private long completeBooking(String customerToken,
            String supplierToken, long listingId) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"listingId":%d,"startTime":"2026-11-01T15:00:00Z",\
                        "endTime":"2026-11-02T15:00:00Z"}
                        """.formatted(listingId)))
                .andExpect(status().isCreated())
                .andReturn();

        Number bookingId = JsonPath.read(
                created.getResponse().getContentAsString(), "$.id");
        long id = bookingId.longValue();

        mockMvc.perform(put("/api/bookings/" + id + "/accept")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/bookings/" + id + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearer(customerToken)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/bookings/" + id + "/complete")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken)))
                .andExpect(status().isOk());

        return id;
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
                        {"title":"Kayak","category":"SPORTS","price":25.00,\
                        "location":"Edmonton",\
                        "description":"A day on the river."}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        Number listingId = JsonPath.read(
                result.getResponse().getContentAsString(), "$.id");

        return listingId.longValue();
    }

    /**
     * reviewBody - This function builds the create JSON body.
     *
     * Parameters:
     * rating - The 1-5 star rating
     * comment - The reviewer's note
     *
     * Return - The JSON string
     */
    private static String reviewBody(int rating, String comment) {
        return """
                {"rating":%d,"comment":"%s"}
                """.formatted(rating, comment);
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
