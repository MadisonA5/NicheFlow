/**
 * This program is the BK-03/BK-04 regression test for bookings longer
 * than one day. The listing price is a price per day (the listing
 * card, the create form and the booking page all say "/ day", and the
 * booking page estimates price times days). A five-day booking of a
 * 50.00 listing must be charged the five days, 250.00, on confirm,
 * and refund all of it when cancelled more than 24 hours ahead.
 *
 * File:   MultiDayBookingRefundRegressionTest.java
 * Course: AUCSC 325
 */
package com.nicheflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nicheflow.payment.MockStripePaymentClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class MultiDayBookingRefundRegressionTest {

    private static final String PASSWORD = "correct-horse";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockStripePaymentClient paymentClient;

    /**
     * fiveDayBookingIsChargedAndRefundedForFiveDays - This function
     * books a 50.00-per-day listing for five days, 40 days ahead,
     * confirms and expects a 250.00 charge, then cancels and expects
     * 250.00 back.
     */
    @Test
    void fiveDayBookingIsChargedAndRefundedForFiveDays() throws Exception {
        String supplier = login("SUPPLIER");
        String customer = login("CUSTOMER");
        long listingId = createListing(supplier, "50.00");

        Instant start = Instant.now().plus(40, ChronoUnit.DAYS)
                .truncatedTo(ChronoUnit.DAYS);
        Instant end = start.plus(5, ChronoUnit.DAYS);
        long bookingId = book(customer, listingId, start, end);

        mockMvc.perform(put("/api/bookings/" + bookingId + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk());

        assertThat(paymentClient.chargeFor("booking-" + bookingId
                + "-confirm").amount()).isEqualByComparingTo("250.00");

        mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(customer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(250.00));
    }

    private String login(String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID()
                + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"%s"}
                        """.formatted(email, PASSWORD, role)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(),
                "$.token");
    }

    private long createListing(String token, String price)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Kayak","category":"SPORTS","price":%s,\
                        "location":"Edmonton","description":"A day on the river."}
                        """.formatted(price)))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(),
                "$.id");
        return id.longValue();
    }

    private long book(String token, long listingId, Instant start,
            Instant end) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"listingId":%d,"startTime":"%s","endTime":"%s"}
                        """.formatted(listingId, start, end)))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(),
                "$.id");
        return id.longValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
