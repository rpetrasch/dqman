package org.dqman.config;

import java.util.List;
import java.util.Map;

import org.dqman.model.DataIntegration;
import org.dqman.model.DqFlow;
import org.dqman.model.DqRule;
import org.dqman.model.StepData;
import org.dqman.repository.DqFlowRepository;
import org.dqman.repository.DataIntegrationRepository;
import org.dqman.repository.DqProjectRepository;
import org.dqman.repository.DqRuleRepository;
import org.dqman.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * DataLoader is a configuration class that is used to load test data into the
 * database.
 * It is used to initialize the database with test data.
 */
@Configuration
@Slf4j
public class DataLoader {

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    private IntegrationDataCreator integrationDataCreator;

    @Autowired
    private RuleDataCreator ruleDataCreator;

    @Autowired
    private FlowDataCreator flowDataCreator;

    @Autowired
    private ProjectDataCreator projectDataCreator;

    /**
     * initDatabase is a bean that is used to initialize the database with test
     * data.
     * 
     * @param dqProjectRepository
     * @param dataIntegrationRepository
     * @param dqRuleRepository
     * @param dqFlowRepository
     * @return CommandLineRunner
     */
    @Bean
    @Transactional
    CommandLineRunner initDatabase(DqProjectRepository dqProjectRepository,
            DataIntegrationRepository dataIntegrationRepository,
            DqRuleRepository dqRuleRepository,
            DqFlowRepository dqFlowRepository) {
        return args -> {
            // Create test rules
            List<DqRule> rules = ruleDataCreator.createRules(dqRuleRepository);

            // Create test integrations
            List<DataIntegration> integrations = integrationDataCreator.createIntegrations(dataIntegrationRepository);
            // Fetch metadata for integration 2 (postgres crm test database)
            DataIntegration i2 = dataIntegrationRepository.findById(2L).get();
            List<String> metadata2 = integrationService.getMetadata(i2);
            log.info("Metadata for integration 2: {}", metadata2);

            // Fetch metadata for integration 4 (csv customer data)
            DataIntegration i4 = dataIntegrationRepository.findById(4L).get();
            List<String> metadata4 = integrationService.getMetadata(i4);
            log.info("Metadata for integration 4: {}", metadata4);
            // Fetch data for integration 2 (postgres crm test database)
            StepData data2 = integrationService.getData(i2, metadata2);
            log.info("Data for integration 2: {}", data2);
            // Fetch data for integration 4 (csv customer data)
            StepData data4 = integrationService.getData(i4, metadata4);
            log.info("Data for integration 4: {}", data4);

            DqFlow flow = flowDataCreator.createFlows(dqFlowRepository, integrations, rules);
            projectDataCreator.createProjects(dqProjectRepository, flow);

        };

    }
}
