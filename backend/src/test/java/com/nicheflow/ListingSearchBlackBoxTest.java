/**
 * This program is the Assignment 4 black-box test suite for LST-02
 * listing search (ListingService.search, reached through
 * GET /api/listings). It never inspects internals: each test seeds
 * listings, calls the endpoint, and checks which listings come back.
 * Three techniques are kept in separate sections: boundary value
 * testing, equivalence class partitioning, and pairwise
 * combinatorial testing. Needs Docker (Testcontainers).
 *
 * File:   ListingSearchBlackBoxTest.java
 * Course: AUCSC 325
 */
package com.nicheflow;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ListingSearchBlackBoxTest {

    private static final String PASSWORD = "correct-horse";

    @Autowired
    private MockMvc mockMvc;

    /** One seeded listing: what we created, kept as the test oracle. */
    private record Seed(long id, String title, String category,
            String price) { }

    // =================================================================
    // SECTION 1: BOUNDARY VALUE TESTING
    // =================================================================

    /**
     * B1-B4 - Price bounds are inclusive. Listings priced 10.00,
     * 25.00 and 50.00; each row is {min, max, expected prices}.
     */
    @ParameterizedTest(name = "min={0} max={1} -> prices {2}")
    @CsvSource(delimiter = '|', value = {
            "25.00|25.00|25.00",          // B1 min == max, hits exactly
            "25.00|50.00|25.00 50.00",    // B2 lower bound inclusive
            "10.00|25.00|10.00 25.00",    // B3 upper bound inclusive
            "10.01|49.99|25.00",          // B4 one cent inside each end
            "50.01|60.00|",               // B5 one cent past top: none
    })
    void priceBoundsAreInclusive(String min, String max,
            String expectedPrices) throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        List<Seed> seeds = List.of(
                seed(token, "P " + marker, "SPORTS", "10.00"),
                seed(token, "P " + marker, "SPORTS", "25.00"),
                seed(token, "P " + marker, "SPORTS", "50.00"));
        Map<String, String> params = params(marker);
        params.put("minPrice", min);
        params.put("maxPrice", max);

        Set<Long> actual = idsOf(search(token, params)
                .andExpect(status().isOk()).andReturn());

        Set<String> wanted = expectedPrices == null ? Set.of()
                : Set.of(expectedPrices.split(" "));
        Set<Long> expected = seeds.stream()
                .filter(s -> wanted.contains(s.price()))
                .map(Seed::id).collect(Collectors.toSet());
        assertThat(actual).isEqualTo(expected);
    }

    /** B6-B7 - Inverted range: one cent over is 400, equal is fine. */
    @Test
    void minOneCentAboveMaxIsRejected() throws Exception {
        String token = loginAsNewSupplier();
        Map<String, String> params = params(marker());
        params.put("minPrice", "25.01");
        params.put("maxPrice", "25.00");
        search(token, params).andExpect(status().isBadRequest());
    }

    /** B8-B11 - Price parameter limits (>= 0.00, at most 8.2 digits). */
    @ParameterizedTest(name = "minPrice={0} -> HTTP {1}")
    @CsvSource({
            "0.00,200",          // B8 lowest legal value
            "-0.01,400",         // B9 one cent below the legal minimum
            "99999999.99,200",   // B10 largest value that fits 8.2 digits
            "100000000.00,400",  // B11 one over: 9 integer digits
    })
    void priceParameterLimits(String minPrice, int expectedStatus)
            throws Exception {
        String token = loginAsNewSupplier();
        Map<String, String> params = params(marker());
        params.put("minPrice", minPrice);
        search(token, params).andExpect(status().is(expectedStatus));
    }

    /** B12-B15 - Page size boundaries: 0 -> 20, 1, 100, 101 -> 100. */
    @ParameterizedTest(name = "size={0} -> reported size {1}")
    @CsvSource({"0,20", "1,1", "100,100", "101,100"})
    void pageSizeBoundaries(String requested, int reported)
            throws Exception {
        String token = loginAsNewSupplier();
        Map<String, String> params = params(marker());
        params.put("size", requested);
        search(token, params).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(reported));
    }

    /**
     * B16-B19 - Page number boundaries with 3 listings and size 1:
     * page -1 behaves as page 0; page 2 is the last full page;
     * page 3 is just past the end and is empty.
     */
    @ParameterizedTest(name = "page={0} -> reported page {1}, {2} item(s)")
    @CsvSource({"-1,0,1", "0,0,1", "2,2,1", "3,3,0"})
    void pageNumberBoundaries(String requested, int reportedPage,
            int itemCount) throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        for (int i = 0; i < 3; i++) {
            seed(token, "Pg " + marker, "SPORTS", "5.00");
        }
        Map<String, String> params = params(marker);
        params.put("size", "1");
        params.put("page", requested);
        search(token, params).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(reportedPage))
                .andExpect(jsonPath("$.items.length()").value(itemCount))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    // =================================================================
    // SECTION 2: EQUIVALENCE CLASS PARTITIONING
    // Seed data: "Alpha"/SPORTS, "Alpha"/WATER, "Beta"/SPORTS
    // =================================================================

    /** E1 - Keyword class "ordinary text": case-insensitive substring. */
    @Test
    void keywordMatchesSubstringIgnoringCase() throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        Seed a = seed(token, "Red Kayak " + marker, "SPORTS", "20.00");
        seed(token, "Blue Canoe " + marker, "SPORTS", "20.00");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", ("kayak " + marker).toLowerCase());
        assertThat(idsOf(search(token, params).andExpect(status().isOk())
                .andReturn())).containsExactly(a.id());
    }

    /** E2 - Keyword class "contains % wildcard": taken literally. */
    @Test
    void percentSignInKeywordIsLiteral() throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        Seed literal = seed(token, "Deal 100% " + marker, "SPORTS", "9.00");
        seed(token, "Deal 1000 " + marker, "SPORTS", "9.00");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", "100% " + marker);
        assertThat(idsOf(search(token, params).andExpect(status().isOk())
                .andReturn())).containsExactly(literal.id());
    }

    /** E3 - Keyword class "contains _ wildcard": taken literally. */
    @Test
    void underscoreInKeywordIsLiteral() throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        Seed literal = seed(token, "a_b " + marker, "SPORTS", "9.00");
        seed(token, "axb " + marker, "SPORTS", "9.00");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", "a_b " + marker);
        assertThat(idsOf(search(token, params).andExpect(status().isOk())
                .andReturn())).containsExactly(literal.id());
    }

    /** E4 - Keyword class "blank": filter skipped, request succeeds. */
    @Test
    void blankKeywordAppliesNoTitleFilter() throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        seed(token, "Any " + marker, "SPORTS", "9.00");
        seed(token, "Any " + marker, "WATER", "9.00");

        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", "   ");
        search(token, params).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements",
                        greaterThanOrEqualTo(2)));
    }

    /** E5 - Keyword class "matches nothing": empty page, still 200. */
    @Test
    void keywordMatchingNothingReturnsEmptyPage() throws Exception {
        String token = loginAsNewSupplier();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", "nomatch-" + UUID.randomUUID());
        search(token, params).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    /**
     * E6-E9 - Category classes: exact, wrong case, padded with
     * spaces, blank, and a category nobody uses.
     */
    @ParameterizedTest(name = "category=''{0}'' -> {1} match(es)")
    @CsvSource(quoteCharacter = '"', value = {
            "\"SPORTS\",2",        // E6 exact match
            "\"sports\",0",        // E7 wrong case: match is exact
            "\"  SPORTS  \",2",    // E8 padding trimmed
            "\"  \",3",            // E9 blank: filter skipped
            "\"TOYS\",0",          // E10 unused category
    })
    void categoryClasses(String category, int expectedCount)
            throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        seed(token, "Alpha " + marker, "SPORTS", "10.00");
        seed(token, "Alpha " + marker, "WATER", "10.00");
        seed(token, "Beta " + marker, "SPORTS", "10.00");

        Map<String, String> params = params(marker);
        params.put("category", category);
        search(token, params).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements")
                        .value(expectedCount));
    }

    /** E11-E13 - Invalid price classes: inverted, negative, non-numeric. */
    @ParameterizedTest(name = "min={0} max={1} -> 400")
    @CsvSource({"50.00,10.00", "-5.00,", "foo,"})
    void invalidPriceInputsAreRejected(String min, String max)
            throws Exception {
        String token = loginAsNewSupplier();
        Map<String, String> params = params(marker());
        params.put("minPrice", min);
        if (max != null) {
            params.put("maxPrice", max);
        }
        search(token, params).andExpect(status().isBadRequest());
    }

    // =================================================================
    // SECTION 3: COMBINATORIAL (PAIRWISE) TESTING
    // keyword {ALL, ALPHA, BETA} x category {ANY, SPORTS, WATER}
    // x price {none, min only, max only, both}. 36 full combinations
    // reduced to 12 rows that still cover every pair of values.
    // Seed: Alpha/SPORTS/10, Alpha/WATER/30, Beta/SPORTS/30,
    //       Beta/WATER/50.
    // =================================================================

    static Stream<Arguments> pairwiseRows() {
        String[] keywords = {"ALL", "ALPHA", "BETA"};
        String[] categories = {"ANY", "SPORTS", "WATER"};
        String[][] prices = {{null, null}, {"30.00", null},
                {null, "30.00"}, {"20.00", "40.00"}};
        List<Arguments> rows = new ArrayList<>();
        for (int p = 0; p < prices.length; p++) {
            for (int k = 0; k < keywords.length; k++) {
                // (k + p) % 3 makes every keyword/category and
                // category/price pair appear at least once
                int c = (k + p) % categories.length;
                rows.add(Arguments.of(keywords[k], categories[c],
                        prices[p][0], prices[p][1]));
            }
        }
        return rows.stream();
    }

    @ParameterizedTest(name = "keyword={0} category={1} min={2} max={3}")
    @MethodSource("pairwiseRows")
    void pairwiseFilterCombinations(String keywordCode, String category,
            String min, String max) throws Exception {
        String token = loginAsNewSupplier();
        String marker = marker();
        List<Seed> seeds = List.of(
                seed(token, "Alpha " + marker, "SPORTS", "10.00"),
                seed(token, "Alpha " + marker, "WATER", "30.00"),
                seed(token, "Beta " + marker, "SPORTS", "30.00"),
                seed(token, "Beta " + marker, "WATER", "50.00"));

        String keyword = switch (keywordCode) {
            case "ALPHA" -> "Alpha " + marker;
            case "BETA" -> "Beta " + marker;
            default -> marker;   // "ALL": only the isolation marker
        };
        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", keyword);
        if (!"ANY".equals(category)) {
            params.put("category", category);
        }
        if (min != null) {
            params.put("minPrice", min);
        }
        if (max != null) {
            params.put("maxPrice", max);
        }

        // Oracle: apply the specification to the seed list by hand
        Set<Long> expected = seeds.stream()
                .filter(s -> s.title().toLowerCase()
                        .contains(keyword.toLowerCase()))
                .filter(s -> "ANY".equals(category)
                        || s.category().equals(category))
                .filter(s -> min == null || new BigDecimal(s.price())
                        .compareTo(new BigDecimal(min)) >= 0)
                .filter(s -> max == null || new BigDecimal(s.price())
                        .compareTo(new BigDecimal(max)) <= 0)
                .map(Seed::id).collect(Collectors.toSet());

        assertThat(idsOf(search(token, params).andExpect(status().isOk())
                .andReturn())).isEqualTo(expected);
    }

    // =================================================================
    // HELPERS
    // =================================================================

    /** A unique tag so each test only sees the rows it created. */
    private static String marker() {
        return "T" + UUID.randomUUID();
    }

    /** Query params that restrict the search to one test's rows. */
    private static Map<String, String> params(String marker) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("keyword", marker);
        return params;
    }

    private org.springframework.test.web.servlet.ResultActions search(
            String token, Map<String, String> params) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        params.forEach(request::param);
        return mockMvc.perform(request);
    }

    private static Set<Long> idsOf(MvcResult result) throws Exception {
        List<Number> ids = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.items[*].id");
        return ids.stream().map(Number::longValue)
                .collect(Collectors.toSet());
    }

    private Seed seed(String token, String title, String category,
            String price) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/listings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"%s","category":"%s","price":%s}
                        """.formatted(title, category, price)))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(
                result.getResponse().getContentAsString(), "$.id");
        return new Seed(id.longValue(), title, category, price);
    }

    private String loginAsNewSupplier() throws Exception {
        String email = "supplier-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"SUPPLIER"}
                        """.formatted(email, PASSWORD)))
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
}
