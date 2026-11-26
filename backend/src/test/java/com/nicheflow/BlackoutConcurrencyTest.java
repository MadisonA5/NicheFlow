/**
 * This program proves BO-01 against a real PostgreSQL: two overlapping
 * blackout creates on the same listing at once produce one stored
 * blackout, and the other request fails with a conflict. It needs
 * Docker.
 *
 * File:   BlackoutConcurrencyTest.java
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
import com.nicheflow.repository.ListingBlackoutRepository;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BlackoutConcurrencyTest {

    private static final String PASSWORD = "correct-horse";
    private static final String WINDOW_START = "2026-11-01T00:00:00Z";
    private static final String WINDOW_END = "2026-11-05T00:00:00Z";
    private static final int ATTEMPT_COUNT = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingBlackoutRepository listingBlackoutRepository;

    /**
     * overlappingConcurrentBlackoutsLetExactlyOneSucceed - This
     * function releases two blackout creates for the same listing
     * and window at the same moment. Exactly one insert commits. The
     * other is a conflict, and the table holds one row.
     */
    @Test
    void overlappingConcurrentBlackoutsLetExactlyOneSucceed()
            throws Exception {
        String supplierToken = loginAsNewUser("SUPPLIER");
        long listingId = createListing(supplierToken);
        String body = blackoutBody(WINDOW_START, WINDOW_END);

        ExecutorService pool = Executors.newFixedThreadPool(
                ATTEMPT_COUNT);
        CountDownLatch ready = new CountDownLatch(ATTEMPT_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        try {
            // To arm both requests, then release them together
            Future<Integer> firstStatus = pool.submit(() ->
                    postBlackout(supplierToken, listingId, body, ready,
                            start));
            Future<Integer> secondStatus = pool.submit(() ->
                    postBlackout(supplierToken, listingId, body, ready,
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

            assertThat(listingBlackoutRepository.findByListingId(
                    listingId)).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    /**
     * aLaterOverlappingBlackoutIsRejected - This function stores a
     * blackout, then sends a second request that overlaps it, and
     * expects the conflict after the first row has committed.
     */
    @Test
    void aLaterOverlappingBlackoutIsRejected() throws Exception {
        String supplierToken = loginAsNewUser("SUPPLIER");
        long listingId = createListing(supplierToken);

        mockMvc.perform(post("/api/listings/" + listingId + "/blackouts")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blackoutBody(WINDOW_START, WINDOW_END)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/listings/" + listingId + "/blackouts")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blackoutBody("2026-11-02T00:00:00Z",
                        "2026-11-06T00:00:00Z")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Blackout conflict"));

        assertThat(listingBlackoutRepository.findByListingId(listingId))
                .hasSize(1);
    }

    /**
     * aBlackoutThatStartsWhenAnotherEndsSucceeds - This function
     * stores two touching windows. The range is half-open, so the
     * second blackout is not an overlap.
     */
    @Test
    void aBlackoutThatStartsWhenAnotherEndsSucceeds() throws Exception {
        String supplierToken = loginAsNewUser("SUPPLIER");
        long listingId = createListing(supplierToken);

        mockMvc.perform(post("/api/listings/" + listingId + "/blackouts")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blackoutBody(WINDOW_START, WINDOW_END)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/listings/" + listingId + "/blackouts")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(blackoutBody(WINDOW_END, "2026-11-06T00:00:00Z")))
                .andExpect(status().isCreated());

        assertThat(listingBlackoutRepository.findByListingId(listingId))
                .hasSize(2);
    }

    /**
     * postBlackout - This function waits until both callers are
     * ready, then POSTs one blackout and returns the HTTP status.
     *
     * Parameters:
     * token - The supplier JWT
     * listingId - The listing to black out
     * body - The blackout JSON
     * ready - The latch this caller counts down before waiting
     * start - The latch that releases both callers together
     *
     * Return - The HTTP status from the create call
     */
    private int postBlackout(String token, long listingId, String body,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        boolean released = start.await(10, TimeUnit.SECONDS);
        if (!released) {
            return -1;
        }

        MvcResult result = mockMvc.perform(
                post("/api/listings/" + listingId + "/blackouts")
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
        String email = "bo-" + UUID.randomUUID() + "@example.com";

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
     * createListing - This function POSTs a listing and returns the
     * generated id so a blackout can name it.
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
     * blackoutBody - This function builds the create JSON body.
     *
     * Parameters:
     * startTime - The blackout start as an ISO-8601 UTC string
     * endTime - The blackout end as an ISO-8601 UTC string
     *
     * Return - The JSON string
     */
    private static String blackoutBody(String startTime, String endTime) {
        return """
                {"startTime":"%s","endTime":"%s"}
                """.formatted(startTime, endTime);
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
