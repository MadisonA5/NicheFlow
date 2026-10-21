/**
 * This program is the ADM-01 users payload: one page of accounts
 * plus the page number, size, and totals the admin console needs
 * to walk the result set.
 *
 * File:   AdminUserPageResponse.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.nicheflow.model.User;

public record AdminUserPageResponse(List<AdminUserResponse> items,
        int page, int size, long totalElements, int totalPages) {

    /**
     * from - This function maps a Spring page of users into the
     * admin list body.
     *
     * Parameters:
     * result - The page returned by the user query
     *
     * Return - The items plus pagination fields
     */
    public static AdminUserPageResponse from(Page<User> result) {
        List<AdminUserResponse> items = result.getContent().stream()
                .map(AdminUserResponse::from)
                .toList();

        return new AdminUserPageResponse(items, result.getNumber(),
                result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }
}
