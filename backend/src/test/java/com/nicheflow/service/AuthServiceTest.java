package com.nicheflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nicheflow.TestUsers;
import com.nicheflow.exception.DuplicateEmailException;
import com.nicheflow.exception.InvalidCredentialsException;
import com.nicheflow.exception.RoleNotSelfAssignableException;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

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

    @Test
    void registerStoresHashedPasswordAndNeverTheRawOne() {
        given(userRepository.existsByEmail("ada@example.com")).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(call -> call.getArgument(0));

        authService.register("ada@example.com", RAW_PASSWORD, Role.CUSTOMER);

        verify(userRepository).saveAndFlush(savedUser.capture());
        assertThat(savedUser.getValue().getPasswordHash())
                .isEqualTo(HASHED_PASSWORD)
                .isNotEqualTo(RAW_PASSWORD);
    }

    @Test
    void registerNormalisesEmailCaseAndWhitespace() {
        given(userRepository.existsByEmail("ada@example.com")).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
        given(userRepository.saveAndFlush(any(User.class))).willAnswer(call -> call.getArgument(0));

        authService.register("  Ada@Example.COM  ", RAW_PASSWORD, Role.SUPPLIER);

        verify(userRepository).saveAndFlush(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        given(userRepository.existsByEmail("ada@example.com")).willReturn(true);

        assertThatThrownBy(() -> authService.register("ada@example.com", RAW_PASSWORD, Role.CUSTOMER))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void registerTreatsAUniqueIndexViolationAsADuplicateEmail() {
        // The losing side of two simultaneous sign-ups for the same address.
        given(userRepository.existsByEmail("ada@example.com")).willReturn(false);
        given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
        given(userRepository.saveAndFlush(any(User.class)))
                .willThrow(new DataIntegrityViolationException("uk_users_email"));

        assertThatThrownBy(() -> authService.register("ada@example.com", RAW_PASSWORD, Role.CUSTOMER))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void registerRefusesToCreateAnAdminAccount() {
        assertThatThrownBy(() -> authService.register("ada@example.com", RAW_PASSWORD, Role.ADMIN))
                .isInstanceOf(RoleNotSelfAssignableException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void authenticateReturnsTheUserWhenThePasswordMatches() {
        User stored = TestUsers.withId(7L, "ada@example.com", HASHED_PASSWORD, Role.SUPPLIER);
        given(userRepository.findByEmail("ada@example.com")).willReturn(Optional.of(stored));
        given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(true);

        assertThat(authService.authenticate("Ada@example.com", RAW_PASSWORD)).isSameAs(stored);
    }

    @Test
    void authenticateRejectsAWrongPassword() {
        User stored = TestUsers.withId(7L, "ada@example.com", HASHED_PASSWORD, Role.CUSTOMER);
        given(userRepository.findByEmail("ada@example.com")).willReturn(Optional.of(stored));
        given(passwordEncoder.matches("guess", HASHED_PASSWORD)).willReturn(false);

        assertThatThrownBy(() -> authService.authenticate("ada@example.com", "guess"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticateRejectsAnUnknownEmailWithTheSameErrorAsAWrongPassword() {
        given(userRepository.findByEmail("nobody@example.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("nobody@example.com", RAW_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}
