/**
 * This program checks that GET /health stays public so scaffolding and
 * later CI can hit it without a login token.
 *
 * File:   HealthControllerTest.java
 * Course: AUCSC 325

 */
package com.nicheflow.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nicheflow.config.SecurityConfig;
import com.nicheflow.service.JwtService;

@WebMvcTest(HealthController.class)
@Import(SecurityConfig.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    /**
     * healthIsReachableWithoutAuthentication - This function calls GET
     * /health with no Authorization header and expects 200 and status
     * UP so the public permit rule does not regress.
     */
    @Test
    void healthIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
