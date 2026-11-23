/**
 * This program is the black-box test suite for ADM-01, the admin read
 * endpoints GET /api/admin/users and GET /api/admin/bookings, plus the
 * ADMIN role rules around them. Every test goes through the HTTP API
 * with real JWTs against a real PostgreSQL (Testcontainers). Nobody can
 * register as an ADMIN, so the test saves an admin user straight into
 * the database and then logs in through the normal login endpoint.
 * Needs Docker.
 
 */
package com.nicheflow;

import java.util.List;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.UserRepository;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AdminBlackBoxTest {

    private static final String PASSWORD = "correct-horse";
    private static final long MISSING_ID = 999_999_999L;
    private static final String T10 = "2031-04-01T10:00:00Z";
    private static final String T11 = "2031-04-01T11:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // =================================================================
    // ACCESS: who may call the admin endpoints
    // =================================================================

    /** No token at all: 401 on both admin endpoints. */
    @Test
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/bookings"))
                .andExpect(status().isUnauthorized());
    }

    /** A customer and a supplier are both forbidden (403). */
    @Test
    void customerAndSupplierAreForbidden() throws Exception {
        for (String role : List.of("CUSTOMER", "SUPPLIER")) {
            String token = login(role);
            adminGet(token, "/api/admin/users")
                    .andExpect(status().isForbidden());
            adminGet(token, "/api/admin/bookings")
                    .andExpect(status().isForbidden());
        }
    }

    /** An admin can read both lists (200). */
    @Test
    void adminCanReadBothLists() throws Exception {
        String admin = adminToken();
        adminGet(admin, "/api/admin/users").andExpect(status().isOk());
        adminGet(admin, "/api/admin/bookings").andExpect(status().isOk());
    }

    /** A real admin token with its signature tampered with is 401. */
    @Test
    void tamperedAdminTokenIsUnauthorized() throws Exception {
        String[] parts = adminToken().split("\\.");
        String forged = parts[0] + "." + parts[1] + "."
                + new StringBuilder(parts[2]).reverse();

        adminGet(forged, "/api/admin/users")
                .andExpect(status().isUnauthorized());
    }

    /** An admin may not use the customer and supplier write routes. */
    @Test
    void adminCannotUseCustomerOrSupplierWriteRoutes() throws Exception {
        String admin = adminToken();

        mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"listingId":1,"startTime":"%s","endTime":"%s"}
                        """.formatted(T10, T11)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                .contentType(MediaType.APPLICATION_JSON)
                .content(listingBody()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/bookings/" + MISSING_ID + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isForbidden());
    }

    /** Nobody can register as an ADMIN, and no login comes of it. */
    @Test
    void registeringAsAdminIsRejected() throws Exception {
        String email = "wannabe-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"ADMIN"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, PASSWORD)))
                .andExpect(status().is4xxClientError());
    }

    // =================================================================
    // USERS LIST: content
    // =================================================================

    /** A freshly registered customer appears in the list with its role. */
    @Test
    void newCustomerAppearsWithItsRole() throws Exception {
        String email = "listed-" + UUID.randomUUID() + "@example.com";
        register(email, "CUSTOMER");

        assertEquals("CUSTOMER", roleOf(adminToken(), email));
    }

    /** The admin account itself shows up with the ADMIN role. */
    @Test
    void adminAppearsWithTheAdminRole() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        String admin = adminTokenFor(email);

        assertEquals("ADMIN", roleOf(admin, email));
    }

    /** The response never carries a password or a BCrypt hash. */
    @Test
    void userListNeverShowsPasswordsOrHashes() throws Exception {
        login("CUSTOMER");
        String body = adminGet(adminToken(), "/api/admin/users?size=100")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertFalse(body.toLowerCase().contains("password"),
                "response mentions a password field");
        assertFalse(body.contains("$2a$") || body.contains("$2b$")
                || body.contains("$2y$"), "response contains a BCrypt hash");
    }

    // =================================================================
    // PAGING: boundaries (same rules as listing search)
    // =================================================================

    /** Page size: 0 becomes 20, 1 stays 1, 100 stays 100, 101 becomes 100. */
    @ParameterizedTest(name = "size={0} -> reported size {1}")
    @CsvSource({"0,20", "1,1", "100,100", "101,100"})
    void pageSizeBoundaries(String requested, int reported)
            throws Exception {
        adminGet(adminToken(), "/api/admin/users?size=" + requested)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(reported));
    }

    /** A negative page behaves as page 0. */
    @Test
    void negativePageBecomesPageZero() throws Exception {
        adminGet(adminToken(), "/api/admin/users?page=-1&size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0));
    }

    /** A page far past the end is an empty list (200), not an error. */
    @Test
    void pageBeyondTheEndIsEmpty() throws Exception {
        adminGet(adminToken(), "/api/admin/users?page=1000000&size=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements",
                        greaterThanOrEqualTo(1)));
    }

    /** A page that is not a number is rejected (400). */
    @Test
    void nonNumericPageIsRejected() throws Exception {
        adminGet(adminToken(), "/api/admin/users?page=abc")
                .andExpect(status().isBadRequest());
    }

    // =================================================================
    // BOOKINGS LIST and admin cancel
    // =================================================================

    /** A booking made by a customer appears in the admin list. */
    @Test
    void newBookingAppearsInTheAdminList() throws Exception {
        long bookingId = createBooking();

        assertEquals("PENDING", bookingStatus(adminToken(), bookingId));
    }

    /** An admin may cancel someone else's booking, and the list shows it. */
    @Test
    void adminCancelsAnotherUsersBooking() throws Exception {
        long bookingId = createBooking();
        String admin = adminToken();

        mockMvc.perform(put("/api/bookings/" + bookingId + "/cancel")
                .header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertEquals("CANCELLED", bookingStatus(admin, bookingId));
    }

    // =================================================================
    // HELPERS
    // =================================================================

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String listingBody() {
        return """
                {"title":"Kayak %s","category":"SPORTS","price":50.00,"location":"Camrose","description":"Test listing"}
                """.formatted(UUID.randomUUID());
    }

    private ResultActions adminGet(String token, String path)
            throws Exception {
        return mockMvc.perform(get(path)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)));
    }

    private void register(String email, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"%s"}
                        """.formatted(email, PASSWORD, role)))
                .andExpect(status().isCreated());
    }

    private String loginWith(String email) throws Exception {
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

    /** Registers a fresh user with the role and returns their JWT. */
    private String login(String role) throws Exception {
        String email = role.toLowerCase() + "-" + UUID.randomUUID()
                + "@example.com";
        register(email, role);
        return loginWith(email);
    }

    /** Saves an ADMIN straight into the database, then logs in normally. */
    private String adminTokenFor(String email) throws Exception {
        userRepository.save(new User(email,
                passwordEncoder.encode(PASSWORD), Role.ADMIN));
        return loginWith(email);
    }

    private String adminToken() throws Exception {
        return adminTokenFor(
                "admin-" + UUID.randomUUID() + "@example.com");
    }

    /** Walks every page of the user list; returns the role, or null. */
    private String roleOf(String admin, String email) throws Exception {
        int page = 0;
        int totalPages = 1;
        while (page < totalPages) {
            String body = adminGet(admin,
                    "/api/admin/users?size=100&page=" + page)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<String> emails = JsonPath.read(body, "$.items[*].email");
            List<String> roles = JsonPath.read(body, "$.items[*].role");
            int index = emails.indexOf(email);
            if (index >= 0) {
                return roles.get(index);
            }
            Number pages = JsonPath.read(body, "$.totalPages");
            totalPages = pages.intValue();
            page++;
        }
        return null;
    }

    /** Walks every page of the booking list; returns the status, or null. */
    private String bookingStatus(String admin, long bookingId)
            throws Exception {
        int page = 0;
        int totalPages = 1;
        while (page < totalPages) {
            String body = adminGet(admin,
                    "/api/admin/bookings?size=100&page=" + page)
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            List<Number> ids = JsonPath.read(body, "$.items[*].id");
            List<String> statuses = JsonPath.read(body,
                    "$.items[*].status");
            for (int i = 0; i < ids.size(); i++) {
                if (ids.get(i).longValue() == bookingId) {
                    return statuses.get(i);
                }
            }
            Number pages = JsonPath.read(body, "$.totalPages");
            totalPages = pages.intValue();
            page++;
        }
        return null;
    }

    /** A supplier lists something and a customer books it; returns the id. */
    private long createBooking() throws Exception {
        String supplier = login("SUPPLIER");
        MvcResult listing = mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, bearer(supplier))
                .contentType(MediaType.APPLICATION_JSON)
                .content(listingBody()))
                .andExpect(status().isCreated())
                .andReturn();
        Number listingId = JsonPath.read(
                listing.getResponse().getContentAsString(), "$.id");

        String customer = login("CUSTOMER");
        MvcResult booking = mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, bearer(customer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"listingId":%d,"startTime":"%s","endTime":"%s"}
                        """.formatted(listingId.longValue(), T10, T11)))
                .andExpect(status().isCreated())
                .andReturn();
        Number bookingId = JsonPath.read(
                booking.getResponse().getContentAsString(), "$.id");
        return bookingId.longValue();
    }
}
