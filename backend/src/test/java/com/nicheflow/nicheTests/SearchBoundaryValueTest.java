/**
 * This program is the NicheFlow boundary-value suite for
 * ListingService.search. Each relational condition is sampled just
 * outside, on, and just inside the boundary, with every other input
 * held at a nominal value.
 *
 * File:   SearchBoundaryValueTest.java
 * Course: AUCSC 325
 */

package com.nicheflow.nicheTests;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.nicheflow.exception.InvalidSearchException;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.service.ListingService;

@ExtendWith(MockitoExtension.class)
class SearchBoundaryValueTest {

    private static final Sort ID_SORT = Sort.by(Sort.Direction.ASC, "id");

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ListingService listingService;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    /**
     * pageNumberBoundary - This function checks the condition
     * page >= 0. A negative page is outside and becomes 0, 0 is on
     * the boundary, and 1 is the first inside value. Size stays at
     * the nominal value 20.
     *
     * Parameters:
     * page - The page number under test
     * position - Where that page sits relative to the boundary
     * expectedPage - The page number the query must receive
     */
    @ParameterizedTest(name = "page {0} is {1} and is sent as {2}")
    @CsvSource({
            "-1, outside, 0",
            "0, on the boundary, 0",
            "1, inside, 1"
    })
    void pageNumberBoundary(int page, String position, int expectedPage) {
        // To keep the size condition away from its own boundaries
        givenEmptySearch();

        listingService.search(null, null, null, null, page, 20);

        // To read the page request the service built
        verify(listingRepository).searchActive(eq(""), eq(""), eq(false),
                eq(BigDecimal.ZERO), eq(false), eq(BigDecimal.ZERO),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber())
                .as(position)
                .isEqualTo(expectedPage);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(ID_SORT);
    }

    /**
     * pageSizeBoundary - This function checks size >= 1 and
     * size <= 100. Values are the point just below the lower
     * boundary, the lower boundary, an interior point, the upper
     * boundary, and the point just above it. Page stays at 1.
     *
     * Parameters:
     * size - The requested page size
     * position - Where that size sits relative to the two boundaries
     * expectedSize - The size the query must receive
     */
    @ParameterizedTest(name = "size {0} is {1} and is sent as {2}")
    @CsvSource({
            "0, outside below, 20",
            "1, on the lower boundary, 1",
            "50, inside, 50",
            "100, on the upper boundary, 100",
            "101, outside above, 100"
    })
    void pageSizeBoundary(int size, String position, int expectedSize) {
        // To keep the page condition on an inside value
        givenEmptySearch();

        listingService.search(null, null, null, null, 1, size);

        verify(listingRepository).searchActive(eq(""), eq(""), eq(false),
                eq(BigDecimal.ZERO), eq(false), eq(BigDecimal.ZERO),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize())
                .as(position)
                .isEqualTo(expectedSize);
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
    }

    /**
     * priceRangeBoundary - This function checks the condition
     * minPrice > maxPrice. The step is one cent: just inside
     * (min < max), on the inclusive boundary (min == max), and just
     * outside (min > max). A missing bound makes the comparison
     * inapplicable, so those rows must still run the query.
     *
     * Parameters:
     * minPrice - The inclusive lower price, or null
     * maxPrice - The inclusive upper price, or null
     * rejected - True when the range must be refused
     */
    @ParameterizedTest(name = "min {0} max {1} rejected={2}")
    @MethodSource("priceRangeBoundaries")
    void priceRangeBoundary(BigDecimal minPrice, BigDecimal maxPrice,
            boolean rejected) {
        if (rejected) {
            // To require the error instead of an empty page
            assertThatThrownBy(() -> listingService.search(null, null,
                    minPrice, maxPrice, null, null))
                    .isInstanceOf(InvalidSearchException.class)
                    .hasMessage("minPrice cannot be greater than maxPrice");

            verify(listingRepository, never()).searchActive(any(), any(),
                    anyBoolean(), any(), anyBoolean(), any(), any());
            return;
        }

        given(listingRepository.searchActive(eq(""), eq(""),
                eq(minPrice != null), samePrice(minPrice),
                eq(maxPrice != null), samePrice(maxPrice),
                any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));

        // To accept an inclusive range and a one-sided bound
        assertThat(listingService.search(null, null, minPrice, maxPrice,
                null, null).getContent()).isEmpty();

        verify(listingRepository).searchActive(eq(""), eq(""),
                eq(minPrice != null), samePrice(minPrice),
                eq(maxPrice != null), samePrice(maxPrice),
                any(Pageable.class));
    }

    /**
     * priceRangeBoundaries - This function lists the points around
     * the minPrice > maxPrice condition, plus the two cases where
     * one bound is absent.
     *
     * Return - Rows of minPrice, maxPrice, and whether the call fails
     */
    private static Stream<Arguments> priceRangeBoundaries() {
        BigDecimal justBelow = new BigDecimal("9.99");
        BigDecimal onBoundary = new BigDecimal("10.00");
        BigDecimal justAbove = new BigDecimal("10.01");

        return Stream.of(
                Arguments.of(justBelow, onBoundary, false),
                Arguments.of(onBoundary, onBoundary, false),
                Arguments.of(justAbove, onBoundary, true),
                Arguments.of(onBoundary, null, false),
                Arguments.of(null, onBoundary, false));
    }

    /**
     * givenEmptySearch - This function stubs the repository so a
     * valid search returns an empty page. Null filters are allowed
     * because omitted keyword, category, and prices use sentinels.
     */
    private void givenEmptySearch() {
        given(listingRepository.searchActive(eq(""), eq(""), anyBoolean(),
                any(BigDecimal.class), anyBoolean(), any(BigDecimal.class),
                any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of()));
    }

    /**
     * samePrice - This function matches a bound or its zero sentinel.
     *
     * Parameters:
     * price - The bound to match, or null when the zero sentinel is used
     *
     * Return - The matcher for the bound or its zero sentinel
     */
    private static BigDecimal samePrice(BigDecimal price) {
        return eq(price == null ? BigDecimal.ZERO : price);
    }
}
