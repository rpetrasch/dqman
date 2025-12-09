package org.dqman.config;

import org.dqman.repository.DqProjectRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import org.dqman.model.DqFlow;
import org.dqman.model.DqProject;
import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * ProjectDataCreator is a class that is used to create test data for projects.
 * It is used to initialize the database with test data.
 */
@Slf4j
@Component
public class ProjectDataCreator {

        @Transactional
        public void createProjects(DqProjectRepository repository, DqFlow flow) {
                if (repository.count() != 0)
                        return;

                DqProject p1 = new DqProject("Customer Data Quality",
                                "Checking customer emails and phone numbers",
                                "SUCCESS",
                                ZonedDateTime.now().minusDays(1), ZonedDateTime.now().minusDays(1).plusHours(1),
                                ZonedDateTime.now().minusDays(1).plusHours(2), flow);

                DqProject p2 = new DqProject("Transaction Validation", "Validating daily transactions",
                                "FAILED",
                                ZonedDateTime.now().minusDays(2), ZonedDateTime.now().minusDays(2).plusHours(1),
                                ZonedDateTime.now().minusDays(2).plusHours(1).plusMinutes(30));

                DqProject p3 = new DqProject("Product Catalog Sync", "Syncing product data from ERP",
                                "STARTED",
                                ZonedDateTime.now().minusHours(5), ZonedDateTime.now().minusHours(1), null);

                DqProject p4 = new DqProject("Legacy Migration", "Migrating legacy data to new system",
                                "CREATED",
                                ZonedDateTime.now().minusDays(5), null, null);

                DqProject p5 = new DqProject("Weekly Report Data", "Preparing data for weekly reports",
                                "SUCCESS",
                                ZonedDateTime.now().minusDays(3), ZonedDateTime.now().minusDays(3).plusHours(2),
                                ZonedDateTime.now().minusDays(3).plusHours(4));

                DqProject p6 = new DqProject("Translation DQ Check DE-EN",
                                "Language Translation DQ Check DE-EN", "SUCCESS",
                                ZonedDateTime.now().minusDays(4), ZonedDateTime.now().minusDays(3).plusHours(2),
                                ZonedDateTime.now().minusDays(1).plusHours(4));

                DqProject p7 = new DqProject("Translation DQ Check DE-FR",
                                "Language Translation DQ Check DE-FR", "SUCCESS",
                                ZonedDateTime.now().minusDays(5), ZonedDateTime.now().minusDays(4).plusHours(2),
                                ZonedDateTime.now().minusDays(2).plusHours(4));

                DqProject p8 = new DqProject("Translation DQ Check DE-ES",
                                "Language Translation DQ Check DE-ES", "FAILED",
                                ZonedDateTime.now().minusDays(5), ZonedDateTime.now().minusDays(4).plusHours(2),
                                ZonedDateTime.now().minusDays(0).plusHours(4));

                repository.saveAll(Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8));
                log.info("Test data for projects initialized");
        }
}