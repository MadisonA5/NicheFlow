/**
 * This program is the AUTH-01 regression test for passwords that are
 * within the 8 to 72 character limit but longer than 72 bytes. BCrypt
 * only accepts 72 bytes, and a non-ASCII character such as é takes
 * two bytes in UTF-8, so a 37-character password of é is 74 bytes.
 * Registration must answer 400 with a password error, not 401.
 *
 * File:   PasswordByteLengthRegressionTest.java
 * Course: AUCSC 325
 */
package com.nicheflow;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PasswordByteLengthRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * passwordOfExactly72BytesIsAccepted - This function registers
     * with 36 é characters, 72 bytes, the largest password BCrypt
     * can hash, and expects the account to be created.
     */
    @Test
    void passwordOfExactly72BytesIsAccepted() throws Exception {
        register("é".repeat(36))
                .andExpect(status().isCreated());
    }

    /**
     * passwordOver72BytesIsAValidationError - This function registers
     * with 37 é characters, 74 bytes but only 37 characters, and
     * expects a 400 that names the password field.
     */
    @Test
    void passwordOver72BytesIsAValidationError() throws Exception {
        register("é".repeat(37))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    /**
     * register - This function posts a customer registration with a
     * fresh email and the given password.
     *
     * Parameters:
     * password - The raw password to send
     *
     * Return - The result of the request
     */
    private ResultActions register(String password) throws Exception {
        String email = "bytes-" + UUID.randomUUID() + "@example.com";

        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","role":"CUSTOMER"}
                        """.formatted(email, password)));
    }
}
