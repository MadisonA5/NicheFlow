/**
 * This program starts the backend the same way as BackendApplication
 * but also loads TestcontainersConfiguration so a local run can use
 * a disposable PostgreSQL instead of a machine-installed database.
 *
 * File:   TestBackendApplication.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
 *     - Spring Boot Testcontainers service connections
 */
package com.nicheflow;

import org.springframework.boot.SpringApplication;

public class TestBackendApplication {

    /**
     * main - This function boots BackendApplication with the
     * Testcontainers PostgreSQL bean wired in for local development.
     *
     * Parameters:
     * args - The command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        // To start the app using the same main as production
        // To attach the disposable postgres container as the datasource
        SpringApplication.from(BackendApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
