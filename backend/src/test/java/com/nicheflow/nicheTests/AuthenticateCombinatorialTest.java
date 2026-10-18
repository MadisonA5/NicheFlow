/**
 * This program is the NicheFlow combinatorial suite for
 * AuthService.authenticate. The two inputs are the email class and
 * the password class. Every feasible pair is one test case, and
 * every failure uses the same message.
 *
 * File:   AuthenticateCombinatorialTest.java
 * Course: AUCSC 325
 */
package com.nicheflow.nicheTests;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nicheflow.TestUsers;
import com.nicheflow.exception.InvalidCredentialsException;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthenticateCombinatorialTest {

    private static final String STORED_EMAIL = "ada@example.com";
    private static final String MATCHING_PASSWORD = "correct-horse";
    private static final String OTHER_PASSWORD = "wrong-password";
    private static final String HASHED_PASSWORD = "$2a$10$hashed";
    private static final String FAILURE_MESSAGE =
            "Invalid email or password";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    /**
     * emailAndPasswordCombinations - This function builds the full
     * product of the email classes and the password classes. An
     * unknown email paired with either password is still feasible:
     * the login is rejected either way.
     *
     * Return - Rows of typed email, typed password, and success flag
     */
    private static Stream<Arguments> emailAndPasswordCombinations() {
        return Stream.of(
                Arguments.of("ada@example.com", MATCHING_PASSWORD, true),
                Arguments.of("ada@example.com", OTHER_PASSWORD, false),
                Arguments.of("  Ada@Example.COM  ", MATCHING_PASSWORD,
                        true),
                Arguments.of("  Ada@Example.COM  ", OTHER_PASSWORD,
                        false),
                Arguments.of("nobody@example.com", MATCHING_PASSWORD,
                        false),
                Arguments.of("nobody@example.com", OTHER_PASSWORD,
                        false));
    }

    /**
     * authenticateEachPair - This function runs one cell of the
     * email-by-password grid. A known email whose password matches
     * returns the stored user. Every other pair throws the same
     * invalid-credentials error.
     *
     * Parameters:
     * typedEmail - The email class representative
     * typedPassword - The password class representative
     * expectSuccess - True only for a known email and matching password
     */
    @ParameterizedTest(name = "email [{0}] password [{1}] success={2}")
    @MethodSource("emailAndPasswordCombinations")
    void authenticateEachPair(String typedEmail, String typedPassword,
            boolean expectSuccess) {
        User stored = stubAccountLookup(typedEmail, typedPassword);

        if (expectSuccess) {
            // To accept only the pairs the grid marks as a login
            assertThat(authService.authenticate(typedEmail, typedPassword))
                    .isSameAs(stored);
            return;
        }

        // To keep unknown emails and bad passwords indistinguishable
        assertThatThrownBy(() -> authService.authenticate(typedEmail,
                typedPassword))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(FAILURE_MESSAGE);
    }

    /**
     * stubAccountLookup - This function prepares the repository from
     * the specification: trim and lowercase the email, then match
     * the password only when that address is the stored account.
     *
     * Parameters:
     * typedEmail - The email the caller typed
     * typedPassword - The password the caller typed
     *
     * Return - The stored user when the address is known, otherwise null
     */
    private User stubAccountLookup(String typedEmail,
            String typedPassword) {
        String normalized = typedEmail.trim().toLowerCase(Locale.ROOT);

        if (!STORED_EMAIL.equals(normalized)) {
            given(userRepository.findByEmail(normalized))
                    .willReturn(Optional.empty());
            return null;
        }

        User stored = TestUsers.withId(7L, STORED_EMAIL, HASHED_PASSWORD,
                Role.SUPPLIER);
        given(userRepository.findByEmail(STORED_EMAIL))
                .willReturn(Optional.of(stored));
        given(passwordEncoder.matches(typedPassword, HASHED_PASSWORD))
                .willReturn(MATCHING_PASSWORD.equals(typedPassword));

        return stored;
    }
}
