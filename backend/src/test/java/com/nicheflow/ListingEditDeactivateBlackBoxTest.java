/**
 * This program is the black-box test suite for LST-01 listing edit and
 * deactivate (plus the supplier-only rule on create). Every test goes
 * through the HTTP API with real JWTs against a real PostgreSQL
 * (Testcontainers) and checks status codes, response fields, and what
 * search returns afterwards. Each test makes its own users and listing.
 * Needs Docker.
 *
 *
 */
package com.nicheflow;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ListingEditDeactivateBlackBoxTest {

    private static final String PASSWORD = "correct-horse";
    private static final long MISSING_ID = 999_999_999L;

    @Autowired
    private MockMvc mockMvc;

    // =================================================================
    // CREATE: only suppliers, valid fields
    // =================================================================

    /** A customer may not create a listing (403). */
    @Test
    void customerCannotCreateAListing() throws Exception {
        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(login("CUSTOMER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("Kayak " + UUID.randomUUID(), "SPORTS", "10.00")))
                .andExpect(status().isForbidden());
    }

    /** A blank title is rejected (400). */
    @Test
    void blankTitleOnCreateIsRejected() throws Exception {
        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(login("SUPPLIER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body("", "SPORTS", "10.00")))
                .andExpect(status().isBadRequest());
    }

    // =================================================================
    // EDIT: equivalence classes on who edits and what they send
    // =================================================================

    /** The owner edits every field and gets the new values back. */
    @Test
    void ownerEditsAllFields() throws Exception {
        String supplier = login("SUPPLIER");
        long id = createListing(supplier, title(), "SPORTS", "10.00");
        String newTitle = title();

        edit(supplier, id, body(newTitle, "WATER", "15.50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value(newTitle))
                .andExpect(jsonPath("$.category").value("WATER"))
                .andExpect(jsonPath("$.price").value(15.50))
                .andExpect(jsonPath("$.active").value(true));
    }

    /** After an edit, search finds the new title and not the old one. */
    @Test
    void searchSeesTheEditedTitleNotTheOldOne() throws Exception {
        String supplier = login("SUPPLIER");
        String oldTitle = title();
        String newTitle = title();
        long id = createListing(supplier, oldTitle, "SPORTS", "10.00");

        edit(supplier, id, body(newTitle, "SPORTS", "10.00"))
                .andExpect(status().isOk());

        assertEquals(1, countByKeyword(supplier, newTitle));
        assertEquals(0, countByKeyword(supplier, oldTitle));
    }

    /** Another supplier cannot edit it (403), and nothing changes. */
    @Test
    void otherSupplierCannotEditAndNothingChanges() throws Exception {
        String owner = login("SUPPLIER");
        String oldTitle = title();
        long id = createListing(owner, oldTitle, "SPORTS", "10.00");

        edit(login("SUPPLIER"), id, body(title(), "WATER", "99.00"))
                .andExpect(status().isForbidden());

        assertEquals(1, countByKeyword(owner, oldTitle));
    }

    /** A customer cannot edit a listing (403). */
    @Test
    void customerCannotEdit() throws Exception {
        String supplier = login("SUPPLIER");
        long id = createListing(supplier, title(), "SPORTS", "10.00");

        edit(login("CUSTOMER"), id, body(title(), "SPORTS", "10.00"))
                .andExpect(status().isForbidden());
    }

    /** A listing id that does not exist is 404. */
    @Test
    void editingAMissingListingIsNotFound() throws Exception {
        edit(login("SUPPLIER"), MISSING_ID, body(title(), "SPORTS", "10.00"))
                .andExpect(status().isNotFound());
    }

    /** Boundary: id 0 breaks the minimum of 1 (400). */
    @Test
    void editingListingIdZeroIsRejected() throws Exception {
        edit(login("SUPPLIER"), 0, body(title(), "SPORTS", "10.00"))
                .andExpect(status().isBadRequest());
    }

    /** Blank title or blank category on edit is rejected (400). */
    @ParameterizedTest(name = "title=''{0}'' category=''{1}'' -> 400")
    @CsvSource(quoteCharacter = '"', value = {
            "\"\",\"SPORTS\"",
            "\"   \",\"SPORTS\"",
            "\"Kayak\",\"\"",
            "\"Kayak\",\"   \""
    })
    void blankTextFieldsOnEditAreRejected(String titleText, String category)
            throws Exception {
        String supplier = login("SUPPLIER");
        long id = createListing(supplier, title(), "SPORTS", "10.00");

        edit(supplier, id, body(titleText, category, "10.00"))
                .andExpect(status().isBadRequest());
    }

    /** Boundary values for the price field on edit. */
    @ParameterizedTest(name = "price {0} -> HTTP {1}")
    @CsvSource({
            "-0.01,400",          // one cent below zero
            "0.00,200",           // lowest allowed price
            "99999999.99,200",    // largest value that fits 8.2 digits
            "100000000.00,400",   // 9 integer digits
            "10.001,400"          // 3 decimal places
    })
    void priceBoundariesOnEdit(String price, int expectedStatus)
            throws Exception {
        String supplier = login("SUPPLIER");
        long id = createListing(supplier, title(), "SPORTS", "10.00");

        edit(supplier, id, body(title(), "SPORTS", price))
                .andExpect(status().is(expectedStatus));
    }

    // =================================================================
    // DEACTIVATE
    // =================================================================

    /** The owner deactivates: active becomes false and search hides it. */
    @Test
    void ownerDeactivatesAndSearchHidesIt() throws Exception {
        String supplier = login("SUPPLIER");
        String title = title();
        long id = createListing(supplier, title, "SPORTS", "10.00");

        deactivate(supplier, id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertEquals(0, countByKeyword(supplier, title));
    }

    /** Another supplier cannot deactivate (403); it stays searchable. */
    @Test
    void otherSupplierCannotDeactivate() throws Exception {
        String owner = login("SUPPLIER");
        String title = title();
        long id = createListing(owner, title, "SPORTS", "10.00");

        deactivate(login("SUPPLIER"), id).andExpect(status().isForbidden());

        assertEquals(1, countByKeyword(owner, title));
    }

    /** A customer cannot deactivate (403). */
    @Test
    void customerCannotDeactivate() throws Exception {
        String supplier = login("SUPPLIER");
        long id = createListing(supplier, title(), "SPORTS", "10.00");

        deactivate(login("CUSTOMER"), id).andExpect(status().isForbidden());
    }

    /** A missing listing is 404, and id 0 is 400. */
    @Test
    void deactivatingMissingOrInvalidIdsIsRejected() throws Exception {
        String supplier = login("SUPPLIER");
        deactivate(supplier, MISSING_ID).andExpect(status().isNotFound());
        deactivate(supplier, 0).andExpect(status().isBadRequest());
    }

    /**
     * Deactivating twice: the spec does not say what should happen, so
     * this only checks the server does not crash (no 5xx) and the
     * listing stays hidden. Record the actual status in your notes.
     */
    @Test
    void deactivatingTwiceDoesNotCrashAndStaysHidden() throws Exception {
        String supplier = login("SUPPLIER");
        String title = title();
        long id = createListing(supplier, title, "SPORTS", "10.00");
        deactivate(supplier, id).andExpect(status().isOk());

        deactivate(supplier, id).andExpect(result -> assertTrue(
                result.getResponse().getStatus() < 500,
                "second deactivate gave "
                        + result.getResponse().getStatus()));

        assertEquals(0, countByKeyword(supplier, title));
    }

    /**
     * Editing a deactivated listing: the spec does not say, so this
     * checks no crash (no 5xx) and that the edit does not bring the
     * listing back into search.
     */
    @Test
    void editingADeactivatedListingDoesNotReactivateIt() throws Exception {
        String supplier = login("SUPPLIER");
        long id = createListing(supplier, title(), "SPORTS", "10.00");
        deactivate(supplier, id).andExpect(status().isOk());
        String newTitle = title();

        edit(supplier, id, body(newTitle, "SPORTS", "10.00"))
                .andExpect(result -> assertTrue(
                        result.getResponse().getStatus() < 500,
                        "edit of inactive listing gave "
                                + result.getResponse().getStatus()));

        assertEquals(0, countByKeyword(supplier, newTitle));
    }

    // =================================================================
    // HELPERS
    // =================================================================

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String title() {
        return "Kayak " + UUID.randomUUID();
    }

    private static String body(String title, String category, String price) {
        return """
                {"title":"%s","category":"%s","price":%s,"location":"Camrose","description":"Test listing"}
                """.formatted(title, category, price);
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

    private long createListing(String supplierToken, String title,
            String category, String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplierToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(title, category, price)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(
                result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private ResultActions edit(String token, long id, String json)
            throws Exception {
        return mockMvc.perform(put("/api/listings/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions deactivate(String token, long id) throws Exception {
        return mockMvc.perform(post("/api/listings/" + id + "/deactivate")
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    /** How many active listings search finds for this title fragment. */
    private int countByKeyword(String token, String keyword)
            throws Exception {
        MvcResult result = mockMvc.perform(get("/api/listings")
                .param("keyword", keyword)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        Number total = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.totalElements");
        return total.intValue();
    }
}
