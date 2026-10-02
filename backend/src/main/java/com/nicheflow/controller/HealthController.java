/**
 * This program exposes a public health-check endpoint so scaffolding
 * and later CI can confirm the backend process is up.
 *
 * File:   HealthController.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    /**
     * health - This function reports that the backend process is running
     * without requiring a login.
     *
     * Return - A small status map for GET /health
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "nicheflow-backend");
    }
}
