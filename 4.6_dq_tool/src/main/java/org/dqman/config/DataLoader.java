package org.dqman.config;

import org.dqman.model.DqProject;
import org.dqman.repository.DqProjectRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;
import java.util.Arrays;

@Configuration
public class DataLoader {

    @Bean
    CommandLineRunner initDatabase(DqProjectRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                DqProject p1 = new DqProject();
                p1.setName("Customer Data Quality");
                p1.setDescription("Checking customer emails and phone numbers");
                p1.setStatus("SUCCESS");
                p1.setCreatedDate(ZonedDateTime.now().minusDays(1));
                p1.setStartedDate(ZonedDateTime.now().minusDays(1).plusHours(1));
                p1.setFinishedDate(ZonedDateTime.now().minusDays(1).plusHours(2));

                DqProject p2 = new DqProject();
                p2.setName("Transaction Validation");
                p2.setDescription("Validating daily transactions");
                p2.setStatus("FAILED");
                p2.setCreatedDate(ZonedDateTime.now().minusDays(2));
                p2.setStartedDate(ZonedDateTime.now().minusDays(2).plusHours(1));
                p2.setFinishedDate(ZonedDateTime.now().minusDays(2).plusHours(1).plusMinutes(30));

                DqProject p3 = new DqProject();
                p3.setName("Product Catalog Sync");
                p3.setDescription("Syncing product data from ERP");
                p3.setStatus("STARTED");
                p3.setCreatedDate(ZonedDateTime.now().minusHours(5));
                p3.setStartedDate(ZonedDateTime.now().minusHours(1));

                DqProject p4 = new DqProject();
                p4.setName("Legacy Migration");
                p4.setDescription("Migrating legacy data to new system");
                p4.setStatus("CREATED");
                p4.setCreatedDate(ZonedDateTime.now().minusDays(5));

                DqProject p5 = new DqProject();
                p5.setName("Weekly Report Data");
                p5.setDescription("Preparing data for weekly reports");
                p5.setStatus("SUCCESS");
                p5.setCreatedDate(ZonedDateTime.now().minusDays(3));
                p5.setStartedDate(ZonedDateTime.now().minusDays(3).plusHours(2));
                p5.setFinishedDate(ZonedDateTime.now().minusDays(3).plusHours(4));

                DqProject p6 = new DqProject();
                p6.setName("Translation DQ Check DE-EN");
                p6.setDescription("Language Translation DQ Check DE-EN");
                p6.setStatus("SUCCESS");
                p6.setCreatedDate(ZonedDateTime.now().minusDays(6));
                p6.setStartedDate(ZonedDateTime.now().minusDays(3).plusHours(2));
                p6.setFinishedDate(ZonedDateTime.now().minusDays(1).plusHours(4));

                DqProject p7 = new DqProject();
                p7.setName("Translation DQ Check DE-FR");
                p7.setDescription("Language Translation DQ Check DE-FR");
                p7.setStatus("SUCCESS");
                p7.setCreatedDate(ZonedDateTime.now().minusDays(4));
                p7.setStartedDate(ZonedDateTime.now().minusDays(4).plusHours(2));
                p7.setFinishedDate(ZonedDateTime.now().minusDays(2).plusHours(4));

                DqProject p8 = new DqProject();
                p8.setName("Translation DQ Check DE-ES");
                p8.setDescription("Language Translation DQ Check DE-ES");
                p8.setStatus("FAILED");
                p8.setCreatedDate(ZonedDateTime.now().minusDays(4));
                p8.setStartedDate(ZonedDateTime.now().minusDays(4).plusHours(2));
                p8.setFinishedDate(ZonedDateTime.now().minusDays(0).minusHours(4));

                repository.saveAll(Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8));
                System.out.println("Test data initialized");
            }
        };
    }
}
