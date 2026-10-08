package com.nicheflow.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nicheflow.TestUsers;
import com.nicheflow.config.SecurityConfig;
import com.nicheflow.exception.DuplicateEmailException;
import com.nicheflow.exception.GlobalExceptionHandler;
import com.nicheflow.exception.InvalidCredentialsException;
import com.nicheflow.exception.RoleNotSelfAssignableException;
import com.nicheflow.model.Role;
import com.nicheflow.security.AuthenticatedUser;
import com.nicheflow.service.AuthService;
import com.nicheflow.service.JwtService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;

@WebMvcTest(AuthController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void registrationReturns201AndNeverEchoesThePasswordHash() throws Exception {
        given(authService.register("ada@example.com", "correct-horse", Role.CUSTOMER))
                .willReturn(TestUsers.withId(1L, "ada@example.com", "$2a$10$hashed", Role.CUSTOMER));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ada@example.com","password":"correct-horse","role":"CUSTOMER"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registrationWithATakenEmailReturns409() throws Exception {
        given(authService.register(any(), any(), any()))
                .willThrow(new DuplicateEmailException("ada@example.com"));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ada@example.com","password":"correct-horse","role":"CUSTOMER"}
                        """))
                .andExpect(status().isConflict());
    }

    @Test
    void registrationWithAShortPasswordReturns400WithTheOffendingField() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ada@example.com","password":"short","role":"CUSTOMER"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void registrationWithAMalformedEmailReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"not-an-email","password":"correct-horse","role":"CUSTOMER"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void registrationAsAdminReturns400() throws Exception {
        given(authService.register(any(), any(), any()))
                .willThrow(new RoleNotSelfAssignableException(Role.ADMIN));

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ada@example.com","password":"correct-horse","role":"ADMIN"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginReturnsABearerTokenForValidCredentials() throws Exception {
        given(authService.authenticate("ada@example.com", "correct-horse"))
                .willReturn(TestUsers.withId(1L, "ada@example.com", "$2a$10$hashed", Role.SUPPLIER));
        given(jwtService.issue(any())).willReturn("a.jwt.token");
        given(jwtService.expiresInSeconds()).willReturn(3600L);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ada@example.com","password":"correct-horse"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("a.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.role").value("SUPPLIER"));
    }

    @Test
    void loginWithBadCredentialsReturns401() throws Exception {
        given(authService.authenticate(any(), any())).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"ada@example.com","password":"guess"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsARequestWithNoToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsAnUnparseableToken() throws Exception {
        given(jwtService.parse("broken")).willThrow(new MalformedJwtException("broken"));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer broken"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * protectedEndpointRejectsAnExpiredToken - This function hits a
     * protected route with an expired JWT through MockMvc so AUTH-01
     * expired-session handling is proven on the HTTP path, not only
     * inside JwtServiceTest.
     */
    @Test
    void protectedEndpointRejectsAnExpiredToken() throws Exception {
        given(jwtService.parse("expired-token"))
                .willThrow(new ExpiredJwtException(null, null, "expired"));

        mockMvc.perform(get("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointResolvesTheCallerFromAValidToken() throws Exception {
        given(jwtService.parse("good-token"))
                .willReturn(new AuthenticatedUser(1L, "ada@example.com", Role.SUPPLIER));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer good-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.role").value("SUPPLIER"));
    }
}
