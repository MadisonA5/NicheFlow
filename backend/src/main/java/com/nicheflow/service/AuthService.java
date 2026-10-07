/**
 * This program implements AUTH-01 registration and login: hash the
 * password, reject duplicate emails, and verify credentials without
 * revealing which accounts exist.
 *
 * File:   AuthService.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html
 *     - BCrypt password hashing
 */
package com.nicheflow.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nicheflow.exception.DuplicateEmailException;
import com.nicheflow.exception.InvalidCredentialsException;
import com.nicheflow.exception.RoleNotSelfAssignableException;
import com.nicheflow.model.Role;
import com.nicheflow.model.User;
import com.nicheflow.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * AuthService - This function stores the repository and encoder
     * Spring injects so register and login can stay in one place.
     *
     * Parameters:
     * userRepository - The users table access point
     * passwordEncoder - The BCrypt hasher used on every password
     */
    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * register - This function creates a new account after hashing the
     * password. It rejects admin self-assignment and duplicate emails.
     *
     * Parameters:
     * email - The address the user signed up with
     * rawPassword - The plaintext password to hash and store
     * role - The marketplace role to assign
     *
     * Return - The persisted user with its generated id
     */
    @Transactional
    public User register(String email, String rawPassword, Role role) {
        // To block clients from creating their own admin accounts
        rejectAdminSelfAssignment(role);

        // To treat Ada@Example.COM and ada@example.com as the same address
        String normalizedEmail = normalize(email);

        // To fail fast when the email is already taken
        rejectDuplicateEmail(normalizedEmail);

        // To store a BCrypt hash instead of the raw password
        User user = new User(normalizedEmail,
                passwordEncoder.encode(rawPassword), role);

        return persistNewUser(user, normalizedEmail);
    }

    /**
     * authenticate - This function verifies an email and password
     * without revealing which of the two was wrong.
     *
     * Parameters:
     * email - The address the caller typed
     * rawPassword - The plaintext password to check
     *
     * Return - The matching stored user
     */
    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        // To look up the account and accept it only when the hash matches
        return userRepository.findByEmail(normalize(email))
                .filter(user -> passwordEncoder.matches(rawPassword,
                        user.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
    }

    /**
     * rejectAdminSelfAssignment - This function stops ADMIN from being
     * chosen during public registration.
     *
     * Parameters:
     * role - The role the client asked for
     */
    private void rejectAdminSelfAssignment(Role role) {
        if (role == Role.ADMIN) {
            throw new RoleNotSelfAssignableException(role);
        }
    }

    /**
     * rejectDuplicateEmail - This function throws when the pre-check
     * finds the email already stored.
     *
     * Parameters:
     * normalizedEmail - The already-normalized address to look up
     */
    private void rejectDuplicateEmail(String normalizedEmail) {
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }
    }

    /**
     * persistNewUser - This function saves the new row and maps a
     * unique-index race to the same duplicate-email error as the
     * pre-check.
     *
     * Parameters:
     * user - The account to insert
     * normalizedEmail - The email used if the unique index fires
     *
     * Return - The flushed user with its generated id
     */
    private User persistNewUser(User user, String normalizedEmail) {
        try {
            // To write the row and force the unique index to run now
            return userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException concurrentInsert) {
            throw new DuplicateEmailException(normalizedEmail);
        }
    }

    /**
     * normalize - This function trims and lowercases an email so the
     * unique constraint cannot be bypassed by case or spaces.
     *
     * Parameters:
     * email - The address as the client typed it
     *
     * Return - The trimmed, lowercased email
     */
    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
