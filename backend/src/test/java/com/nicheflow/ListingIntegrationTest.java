/**
 * This program is the LST-01/LST-02 integration test: create, edit,
 * ownership 403, and search of active listings against a real
 * PostgreSQL started by TestcontainersConfiguration. It needs Docker;
 * the slice tests do not.
 *
 * File:   ListingIntegrationTest.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
 *     - Spring Boot Testcontainers service connections
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ListingIntegrationTest {

    private static final String SUPPLIER_PASSWORD = "correct-horse";

    @Autowired
    private MockMvc mockMvc;

    /**
     * supplierCanCreateAListing - This function registers a supplier,
     * logs in, and POSTs a listing so Flyway, JPA, and JWT all run
     * against the Testcontainers database.
     */
    @Test
    void supplierCanCreateAListing() throws Exception {
        String token = loginAsNewSupplier();

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(listingBody("Red Kayak", "SPORTS", "25.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Red Kayak"))
                .andExpect(jsonPath("$.category").value("SPORTS"))
                .andExpect(jsonPath("$.price").value(25.00))
                .andExpect(jsonPath("$.location").value("Edmonton"))
                .andExpect(jsonPath("$.description")
                        .value("A day on the river."))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.id").isNumber());
    }

    /**
     * supplierCanEditTheirOwnListing - This function creates a listing
     * and PUTs new fields with the same JWT so LST-01 ownership
     * succeeds on a real row.
     */
    @Test
    void supplierCanEditTheirOwnListing() throws Exception {
        String token = loginAsNewSupplier();
        long listingId = createListing(token, "Old Kayak", "SPORTS",
                "10.00");

        mockMvc.perform(put("/api/listings/" + listingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(listingBody("New Kayak", "WATER", "15.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(listingId))
                .andExpect(jsonPath("$.title").value("New Kayak"))
                .andExpect(jsonPath("$.category").value("WATER"))
                .andExpect(jsonPath("$.price").value(15.00));
    }

    /**
     * differentSupplierCannotEditSomeoneElsesListing - This function
     * has a second supplier PUT the first supplier's listing and
     * expects 403, the LST-01 ownership check on a real JWT pair.
     */
    @Test
    void differentSupplierCannotEditSomeoneElsesListing()
            throws Exception {
        String ownerToken = loginAsNewSupplier();
        long listingId = createListing(ownerToken, "Owned Kayak",
                "SPORTS", "25.00");

        String otherToken = loginAsNewSupplier();

        mockMvc.perform(put("/api/listings/" + listingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(otherToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(listingBody("Stolen Kayak", "SPORTS", "1.00")))
                .andExpect(status().isForbidden());
    }

    /**
     * searchReturnsOnlyActiveListings - This function creates two
     * listings, deactivates one, and searches by a unique keyword so
     * only the still-active row is returned.
     */
    @Test
    void searchReturnsOnlyActiveListings() throws Exception {
        String token = loginAsNewSupplier();
        String marker = "IT-" + UUID.randomUUID();

        createListing(token, "Active " + marker, "SPORTS", "25.00");
        long hiddenId = createListing(token, "Hidden " + marker,
                "SPORTS", "30.00");

        // To hide one row without deleting it
        mockMvc.perform(post("/api/listings/" + hiddenId
                + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/listings")
                .param("keyword", marker)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title")
                        .value("Active " + marker))
                .andExpect(jsonPath("$.items[0].active").value(true));
    }

    /**
     * loginAsNewSupplier - This function registers a unique SUPPLIER
     * and logs in so each test has its own JWT and User row.
     *
     * Return - The Bearer token from login
     */
    private String loginAsNewSupplier() throws Exception {
        String email = "supplier-" + UUID.randomUUID()
                + "@example.com";

        // To persist the supplier account through AUTH-01
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"SUPPLIER"}
                        """.formatted(email, SUPPLIER_PASSWORD)))
                .andExpect(status().isCreated());

        // To exchange those credentials for a JWT
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, SUPPLIER_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(loginResult.getResponse().getContentAsString(),
                "$.token");
    }

    /**
     * createListing - This function POSTs a listing and returns the
     * generated id so later edit, deactivate, or search can use it.
     *
     * Parameters:
     * token - The supplier JWT
     * title - The listing title
     * category - The listing category
     * price - The price as a two-decimal string
     *
     * Return - The persisted listing id
     */
    private long createListing(String token, String title,
            String category, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(listingBody(title, category, price)))
                .andExpect(status().isCreated())
                .andReturn();

        Number listingId = JsonPath.read(
                result.getResponse().getContentAsString(), "$.id");

        return listingId.longValue();
    }

    /**
     * listingBody - This function builds the create/edit JSON body.
     *
     * Parameters:
     * title - The listing title
     * category - The listing category
     * price - The price as a two-decimal string
     *
     * Return - The JSON string
     */
    private static String listingBody(String title, String category,
            String price) {
        return """
                {"title":"%s","category":"%s","price":%s,\
                "location":"Edmonton","description":"A day on the river."}
                """.formatted(title, category, price);
    }

    /**
     * bearer - This function prefixes a JWT with the Bearer scheme
     * the filter expects, the same way /api/auth/me does.
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
