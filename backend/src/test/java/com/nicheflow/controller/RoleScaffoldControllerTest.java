/**
 * This program proves AUTH-02 on the temporary admin ping still
 * pending ADM-01: a CUSTOMER token is forbidden and an ADMIN token
 * is allowed. Supplier-role enforcement is now proven by
 * /api/listings. Delete this file with AdminPingController.
 *
 * File:   RoleScaffoldControllerTest.java
 * Course: AUCSC 325 
 *    
 */
package com.nicheflow.controller;

import org.junit.jupiter.api.Test;
import static org.mockito.BDDMockito.given;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nicheflow.config.SecurityConfig;
import com.nicheflow.model.Role;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.JwtService;

@WebMvcTest(AdminPingController.class)
@Import(SecurityConfig.class)
class RoleScaffoldControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    /**
     * customerIsForbiddenOnAdminPing - This function sends a CUSTOMER
     * token to GET /api/admin/ping and expects 403.
     */
    @Test
    void customerIsForbiddenOnAdminPing() throws Exception {
        getWithRole("/api/admin/ping", "customer-token", Role.CUSTOMER)
                .andExpect(status().isForbidden());
    }

    /**
     * adminIsAllowedOnAdminPing - This function sends an ADMIN token
     * to GET /api/admin/ping and expects 200.
     */
    @Test
    void adminIsAllowedOnAdminPing() throws Exception {
        getWithRole("/api/admin/ping", "admin-token", Role.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("admin"));
    }

    /**
     * getWithRole - This function stubs jwtService.parse so the filter
     * builds a ROLE_ authority, then GET the path with that Bearer
     * token.
     *
     * Parameters:
     * path - The placeholder endpoint to call
     * token - The fake compact JWT the filter will parse
     * role - The role the stubbed token should carry
     *
     * Return - The MockMvc result so the test can assert status
     */
    private ResultActions getWithRole(String path, String token, Role role)
            throws Exception {
        // To make the filter treat this token as that role
        given(jwtService.parse(token))
                .willReturn(new AuthenticatedUser(1L, "ada@example.com",
                        role));

        return mockMvc.perform(get(path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
