/**
 * This program starts a disposable PostgreSQL container for tests and
 * the TestBackendApplication entry point so Flyway and JPA can run
 * against a real database without a local install.
 *
 * File:   TestcontainersConfiguration.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
 *     - Spring Boot Testcontainers service connections
 */
package com.nicheflow;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    /**
     * postgresContainer - This function builds a PostgreSQL Testcontainer
     * and marks it as a service connection so Spring wires jdbc url,
     * username, and password from the running container.
     *
     * Return - The started PostgreSQL container bean
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // To pull and run the official postgres image for this test JVM
        return new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    }

}
