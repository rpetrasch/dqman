package org.dqman.config;

import java.util.Arrays;
import org.dqman.model.DqIntegration;
import org.dqman.repository.DqIntegrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * IntegrationDataCreator is a class that is used to create test data for
 * integrations.
 * It is used to initialize the database with test data.
 */
@Component
@Slf4j
public class IntegrationDataCreator {

    @Transactional
    public void createIntegrations(DqIntegrationRepository repository) {
        if (repository.count() == 0) {
            DqIntegration i1 = new DqIntegration();
            i1.setName("CRM Database (docker)");
            i1.setDescription("Customer Relationship Management System (Docker)");
            i1.setType("RDBMS");
            i1.setUrl("jdbc:postgresql://crm-postgres:5432/crm_db");
            i1.setUser("crm_user");
            i1.setPassword("crm_password");

            DqIntegration i2 = new DqIntegration();
            i2.setName("CRM Database (localhost)");
            i2.setDescription("Customer Relationship Management System (localhost)");
            i2.setType("RDBMS");
            i2.setUrl("jdbc:postgresql://localhost:5432/crm_db");
            i2.setUser("crm_user");
            i2.setPassword("crm_password");

            DqIntegration i3 = new DqIntegration();
            i3.setName("ERP System (localhost");
            i3.setDescription("Enterprise Resource Planning");
            i3.setType("RDBMS");
            i3.setUrl("jdbc:oracle:thin:@localhost:1521:xe");
            i3.setUser("erp_user");
            i3.setPassword("erp_pass");

            DqIntegration i4 = new DqIntegration();
            i4.setName("Customer CSV (local file)");
            i4.setDescription("Customer data");
            i4.setType("CSV");
            i4.setUrl("./data/customer.csv");

            DqIntegration i5 = new DqIntegration();
            i5.setName("Web Analytics (S3)");
            i5.setDescription("Website traffic logs");
            i5.setType("CSV");
            i5.setUrl("s3://bucket/logs/web_analytics.csv");

            DqIntegration i6 = new DqIntegration();
            i6.setName("Web Analytics (local file)");
            i6.setDescription("Website traffic logs");
            i6.setType("CSV");
            i6.setUrl("file:/logs/web_analytics.csv");

            repository.saveAll(Arrays.asList(i1, i2, i3, i4, i5, i6));
            log.info("Test data for integrations initialized");
        }
    }
}
