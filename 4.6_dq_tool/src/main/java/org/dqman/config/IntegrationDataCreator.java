package org.dqman.config;

import java.util.Arrays;
import java.util.List;

import org.dqman.model.DataIntegration;
import org.dqman.repository.DataIntegrationRepository;
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
    public List<DataIntegration> createIntegrations(DataIntegrationRepository repository) {
        if (repository.count() == 0) {
            DataIntegration i1 = new DataIntegration();
            i1.setName("CRM Database (docker)");
            i1.setDescription("Customer Relationship Management System (Docker)");
            i1.setType("RDBMS");
            i1.setUrl("jdbc:postgresql://crm-postgres:5432/crm_db");
            i1.setUser("crm_user");
            i1.setPassword("crm_password");

            DataIntegration i2 = new DataIntegration();
            i2.setName("CRM Database (localhost)");
            i2.setDescription("Customer Relationship Management System (localhost)");
            i2.setType("RDBMS");
            i2.setUrl("jdbc:postgresql://localhost:5432/crm_db");
            i2.setUser("crm_user");
            i2.setPassword("crm_password");

            DataIntegration i3 = new DataIntegration();
            i3.setName("ERP System (localhost");
            i3.setDescription("Enterprise Resource Planning");
            i3.setType("RDBMS");
            i3.setUrl("jdbc:oracle:thin:@localhost:1521:xe");
            i3.setUser("erp_user");
            i3.setPassword("erp_pass");

            DataIntegration i4 = new DataIntegration();
            i4.setName("Customer CSV (local file)");
            i4.setDescription("Customer data");
            i4.setType("CSV");
            i4.setUrl("./data/customer.csv");

            DataIntegration i5 = new DataIntegration();
            i5.setName("Web Analytics (S3)");
            i5.setDescription("Website traffic logs");
            i5.setType("CSV");
            i5.setUrl("s3://bucket/logs/web_analytics.csv");

            DataIntegration i6 = new DataIntegration();
            i6.setName("Web Analytics (local file)");
            i6.setDescription("Website traffic logs");
            i6.setType("CSV");
            i6.setUrl("file:/logs/web_analytics.csv");

            DataIntegration i7 = new DataIntegration();
            i7.setName("DQ Check results (local file)");
            i7.setDescription("DQ Check for customer data");
            i7.setType("CSV");
            i7.setUrl("./data/dq_check_customer_results.csv");

            repository.saveAll(Arrays.asList(i1, i2, i3, i4, i5, i6, i7));
            log.info("Test data for integrations initialized");
            return Arrays.asList(i1, i2, i3, i4, i5, i6, i7);
        } else {
            return repository.findAll();
        }
    }
}
