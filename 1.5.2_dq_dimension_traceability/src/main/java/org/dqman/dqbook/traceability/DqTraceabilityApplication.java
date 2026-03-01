package org.dqman.dqbook.traceability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Profile;

/**
 * Main class to start the Spring Boot application.
 */
@SpringBootApplication
public class DqTraceabilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(DqTraceabilityApplication.class, args);
    }
}
