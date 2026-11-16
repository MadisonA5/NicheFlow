/**
 * This program checks that the README's backend run instructions work
 * on every operating system: the macOS/Linux Maven wrapper must be
 * executable, and the README must give a command for macOS/Linux as
 * well as Windows.
 *
 * File:   BackendRunInstructionsTest.java
 * Course: AUCSC 325
 *
 */
package com.nicheflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class BackendRunInstructionsTest {

    private static final Path MAVEN_WRAPPER = Path.of("mvnw");
    private static final Path README = Path.of("..", "README.md");

    /**
     * mavenWrapperIsExecutable - This function checks that ./mvnw can be
     * run directly on macOS and Linux. Windows does not use file
     * permissions, so the check is skipped there.
     */
    @Test
    void mavenWrapperIsExecutable() {
        assumeFalse(System.getProperty("os.name").toLowerCase()
                .contains("win"));

        assertThat(Files.isExecutable(MAVEN_WRAPPER))
                .as("backend/mvnw must be executable so ./mvnw works")
                .isTrue();
    }

    /**
     * readmeGivesARunCommandForEveryOperatingSystem - This function
     * checks the README gives both the Windows and the macOS/Linux
     * command for starting the backend.
     */
    @Test
    void readmeGivesARunCommandForEveryOperatingSystem() throws IOException {
        String readme = Files.readString(README);

        assertThat(readme).contains(".\\mvnw.cmd spring-boot:run");
        assertThat(readme).contains("./mvnw spring-boot:run");
    }
}
