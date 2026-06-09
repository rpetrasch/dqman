package org.dqman.cybersecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Email Session Security — DQ & Cybersecurity Example
 *
 * Demonstrates how data quality techniques apply to cybersecurity:
 * - Three-timestamp model (event_time, ingestion_time, processing_time) for forensic traceability
 * - Raw event immutability + normalized event lineage (raw_event_id FK)
 * - Entity resolution with master data (users, devices, locations) via Caffeine cache
 * - Dual detection: streaming per-event rules + post-session recursive CTE aggregates
 * - Full evidence chain: Alert → Session → Normalized Events → Raw Events → Original Log Line
 */
@SpringBootApplication
@EnableCaching
@EnableScheduling
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
}
