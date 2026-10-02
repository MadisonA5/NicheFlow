/**
 * This program is the smoke test that the Spring context starts with
 * Testcontainers PostgreSQL, Flyway, JPA, and security all wired
 * together. It needs Docker; the slice tests do not.
 *
 * File:   BackendApplicationTests.java
 * Course: AUCSC 325
 *
 * References:
 * https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html
 *     - @SpringBootTest contextLoads
 */
package com.nicheflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BackendApplicationTests {

    /**
     * contextLoads - This function starts the full application context
     * against a disposable PostgreSQL. Spring fails the test if any
     * bean, Flyway migration, or entity mapping cannot load.
     */
    @Test
    void contextLoads() {
        // To let Spring Boot fail the test if the context cannot start
    }

}
