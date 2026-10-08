/**
 * This program is the Spring Data access point for the users table so
 * auth code can look up and persist accounts without writing SQL.
 *
 * File:   UserRepository.java
 * Course: AUCSC 325
 */
package com.nicheflow.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nicheflow.model.User;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * findByEmail - This function looks up a user by their unique email.
     *
     * Parameters:
     * email - The email to search for
     *
     * Return - The matching user, or empty if none exists
     */
    Optional<User> findByEmail(String email);

    /**
     * existsByEmail - This function checks whether an email is already
     * registered so sign-up can reject duplicates.
     *
     * Parameters:
     * email - The email to search for
     *
     * Return - True when a row with that email already exists
     */
    boolean existsByEmail(String email);
}
