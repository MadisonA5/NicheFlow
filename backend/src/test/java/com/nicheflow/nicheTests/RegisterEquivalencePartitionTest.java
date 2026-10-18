/**
 * This program is the NicheFlow equivalence-class suite for
 * AuthService.register. Role is split into self-assignable and
 * forbidden, and email is split into free and already taken after
 * normalization. One or two representatives stand for each class.
 *
 * File:   RegisterEquivalencePartitionTest.java
 * Course: AUCSC 325
 */
package com.nicheflow.nicheTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nicheflow.exception.DuplicateEmailException;
import com.nicheflow.exception.RoleNotSelfAssignableException;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.UserRepository;
import com.nicheflow.service.AuthService;

@ExtendWith(MockitoExtension.class)
class RegisterEquivalencePartitionTest {

    private static final String RAW_PASSWORD = "correct-horse";
    private static final String HASHED_PASSWORD = "$2a$10$hashed";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> savedUser;

    /**
     * validRoleClassPersistsTheChosenRole - This function samples
     * both members of the self-assignable role class. CUSTOMER and
     * SUPPLIER must both be stored, and neither may be rejected.
     *
     * Parameters:
     * roleName - CUSTOMER or SUPPLIER
     */
    @ParameterizedTest(name = "{0} is in the valid role class")
    @CsvSource({"CUSTOMER", "SUPPLIER"})
    void validRoleClassPersistsTheChosenRole(String roleName) {
        Role role = Role.valueOf(roleName);
        given(userRepository.existsByEmail("ada@example.com"))
                .willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD))
                .willReturn(HASHED_PASSWORD);
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(call -> call.getArgument(0));

        // To create the account with the role the client asked for
        User created = authService.register("ada@example.com",
                RAW_PASSWORD, role);

        verify(userRepository).saveAndFlush(savedUser.capture());
        assertThat(created.getRole()).isEqualTo(role);
        assertThat(savedUser.getValue().getEmail())
                .isEqualTo("ada@example.com");
        assertThat(savedUser.getValue().getPasswordHash())
                .isEqualTo(HASHED_PASSWORD)
                .isNotEqualTo(RAW_PASSWORD);
    }

    /**
     * invalidRoleClassRejectsAdmin - This function samples the
     * forbidden role class. ADMIN is the only member, and
     * registration must refuse it before any row is written.
     */
    @Test
    void invalidRoleClassRejectsAdmin() {
        assertThatThrownBy(() -> authService.register(
                "ada@example.com", RAW_PASSWORD, Role.ADMIN))
                .isInstanceOf(RoleNotSelfAssignableException.class)
                .hasMessage("The ADMIN role cannot be assigned "
                        + "through registration");

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    /**
     * newEmailClassNormalizesAndPersists - This function samples the
     * free-email class. The canonical address and a case-and-space
     * variant are the same class because both normalize to an
     * address that is not stored yet.
     *
     * Parameters:
     * typedEmail - The address as the client typed it
     * storedEmail - The normalized address that must be saved
     */
    @ParameterizedTest(name = "[{0}] is a new email stored as {1}")
    @CsvSource(value = {
            "ada@example.com,ada@example.com",
            "'  Ada@Example.COM  ',ada@example.com"
    }, ignoreLeadingAndTrailingWhitespace = false)
    void newEmailClassNormalizesAndPersists(String typedEmail,
            String storedEmail) {
        given(userRepository.existsByEmail(storedEmail)).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD))
                .willReturn(HASHED_PASSWORD);
        given(userRepository.saveAndFlush(any(User.class)))
                .willAnswer(call -> call.getArgument(0));

        // To show both representatives create one normalized account
        authService.register(typedEmail, RAW_PASSWORD, Role.CUSTOMER);

        verify(userRepository).saveAndFlush(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo(storedEmail);
        assertThat(savedUser.getValue().getRole()).isEqualTo(Role.CUSTOMER);
    }

    /**
     * takenEmailClassRejectsWithoutSaving - This function samples
     * the already-taken email class. A canonical duplicate and a
     * case-and-space duplicate must both fail with the same error
     * and must not insert a row.
     *
     * Parameters:
     * typedEmail - An address that normalizes to one already stored
     */
    @ParameterizedTest(name = "[{0}] is already taken")
    @CsvSource(value = {
            "ada@example.com",
            "'  ADA@Example.COM  '"
    }, ignoreLeadingAndTrailingWhitespace = false)
    void takenEmailClassRejectsWithoutSaving(String typedEmail) {
        given(userRepository.existsByEmail("ada@example.com"))
                .willReturn(true);

        assertThatThrownBy(() -> authService.register(typedEmail,
                RAW_PASSWORD, Role.SUPPLIER))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessage("An account already exists for "
                        + "ada@example.com");

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }
}
