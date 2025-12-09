package org.dqman.dqbook.traceability.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class to enable JPA auditing.
 */
@Configuration
@EnableJpaAuditing
public class AuditingConfig {
}
