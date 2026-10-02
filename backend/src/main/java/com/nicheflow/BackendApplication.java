/**
 * This program starts the NicheFlow Spring Boot backend so the REST
 * API, security filter, and JPA layer come up together.
 *
 * File:   BackendApplication.java
 * Course: AUCSC 325
 */
package com.nicheflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

    /**
     * main - This function boots the Spring application context from the
     * command line so the server can accept HTTP requests.
     *
     * Parameters:
     * args - The command-line arguments passed through to Spring Boot
     */
    public static void main(String[] args) {
        // To start the embedded server and load every @Configuration class
        SpringApplication.run(BackendApplication.class, args);
    }

}
