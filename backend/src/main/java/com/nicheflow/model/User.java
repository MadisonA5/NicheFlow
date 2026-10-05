/**
 * This program maps a NicheFlow account to the users table. The table is
 * named users because user is a reserved word in PostgreSQL.
 *
 * File:   User.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /**
     * User - This function gives JPA a no-argument constructor so it can
     * load rows into empty instances.
     */
    protected User() {
        // To leave fields unset until JPA copies the row onto this instance
    }

    /**
     * User - This function builds a new account from the fields the
     * registration path already validated and hashed.
     *
     * Parameters:
     * email - The unique, already-normalized email address
     * passwordHash - The BCrypt hash, never the raw password
     * role - The marketplace role stored on this account
     */
    public User(String email, String passwordHash, Role role) {
        // To store only values the registration path already prepared
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /**
     * getId - This function returns the database-generated identifier.
     *
     * Return - The user id, or null before the row is persisted
     */
    public Long getId() {
        // To return the primary key assigned when the row was saved
        return id;
    }

    /**
     * getEmail - This function returns the stored email address.
     *
     * Return - The user's email
     */
    public String getEmail() {
        // To return the unique login address stored on this account
        return email;
    }

    /**
     * setEmail - This function replaces the stored email address.
     *
     * Parameters:
     * email - The new email to store
     */
    public void setEmail(String email) {
        // To replace the stored email after a later update
        this.email = email;
    }

    /**
     * getPasswordHash - This function returns the stored BCrypt hash.
     *
     * Return - The password hash
     */
    public String getPasswordHash() {
        // To return the hash used for login, never a raw password
        return passwordHash;
    }

    /**
     * setPasswordHash - This function replaces the stored password hash.
     *
     * Parameters:
     * passwordHash - The new BCrypt hash to store
     */
    public void setPasswordHash(String passwordHash) {
        // To store a new BCrypt hash after a password change
        this.passwordHash = passwordHash;
    }

    /**
     * getRole - This function returns the account's marketplace role.
     *
     * Return - CUSTOMER, SUPPLIER, or ADMIN
     */
    public Role getRole() {
        // To return the marketplace role used for access control
        return role;
    }

    /**
     * setRole - This function replaces the account's marketplace role.
     *
     * Parameters:
     * role - The new role to store
     */
    public void setRole(Role role) {
        // To replace the stored role after a later update
        this.role = role;
    }
}
