package com.nicheflow.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nicheflow.config.SecurityConfig;
import com.nicheflow.model.Role;
import com.nicheflow.repository.BookingRepository;
import com.nicheflow.repository.ListingRepository;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.JwtService;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListingRepository listingRepository;

    @MockitoBean
    private BookingRepository bookingRepository;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void customerReceivesOnlyTheirBookings() throws Exception {
        given(jwtService.parse("customer-token"))
                .willReturn(new AuthenticatedUser(9L, "customer@example.com",
                        Role.CUSTOMER));

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings").isEmpty())
                .andExpect(jsonPath("$.bookings").isArray())
                .andExpect(jsonPath("$.bookings").isEmpty());

        verify(bookingRepository).findAllForCustomer(9L);
    }

    @Test
    void supplierReceivesTheirOwnListingsAndBookings() throws Exception {
        given(jwtService.parse("supplier-token"))
                .willReturn(new AuthenticatedUser(7L, "supplier@example.com",
                        Role.SUPPLIER));

        mockMvc.perform(get("/api/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer supplier-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listings").isArray())
                .andExpect(jsonPath("$.listings").isEmpty())
                .andExpect(jsonPath("$.bookings").isArray())
                .andExpect(jsonPath("$.bookings").isEmpty());

        verify(listingRepository).findAllBySupplierId(7L);
        verify(bookingRepository).findAllForSupplier(7L);
    }

    @Test
    void accountDataRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/account"))
                .andExpect(status().isUnauthorized());
    }
}
