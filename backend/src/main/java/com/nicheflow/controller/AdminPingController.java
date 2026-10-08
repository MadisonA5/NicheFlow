/**
 * This program is temporary AUTH-02 scaffolding: GET /api/admin/ping
 * proves ROLE_ADMIN is enforced. Remove this file when the real
 * ADM-01 admin console lands in Week 6.
 *
 * File:   AdminPingController.java
 * Course: AUCSC 325
 */
package com.nicheflow.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminPingController {

    /**
     * ping - This function returns a tiny OK body so tests can prove
     * an ADMIN token is allowed and a CUSTOMER token is not. It is
     * not a product feature.
     *
     * Return - A status map for GET /api/admin/ping
     */
    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("status", "UP", "scope", "admin");
    }
}
