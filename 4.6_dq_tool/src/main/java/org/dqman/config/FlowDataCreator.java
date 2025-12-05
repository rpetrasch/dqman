package org.dqman.config;

import org.dqman.model.DqFlow;
import org.dqman.model.DqFlowStep;
import org.dqman.repository.DqFlowRepository;

import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.util.Arrays;

/**
 * FlowDataCreator is a class that is used to create test data for flows.
 * It is used to initialize the database with test data.
 */
@Slf4j
public class FlowDataCreator {

    public static void createFlows(DqFlowRepository repository) {
        if (repository.count() == 0) {
            DqFlow f1 = new DqFlow();
            f1.setName("Customer Onboarding Flow");
            f1.setDescription("DQ checks for new customers");
            f1.setStatus("PRODUCTION");
            f1.setCreatedDate(ZonedDateTime.now().minusDays(10));
            f1.setModifiedDate(ZonedDateTime.now().minusDays(1));

            DqFlowStep s1 = new DqFlowStep();
            s1.setName("Load CRM Data");
            s1.setDescription("Load data from CRM");
            s1.setType("DATA SOURCE");
            f1.addStep(s1);

            DqFlowStep s2 = new DqFlowStep();
            s2.setName("Validate Emails");
            s2.setDescription("Check email format");
            s2.setType("DQ RULE");
            f1.addStep(s2);

            DqFlow f2 = new DqFlow();
            f2.setName("Daily Transaction Check");
            f2.setDescription("Validate daily transactions");
            f2.setStatus("CREATED");
            f2.setCreatedDate(ZonedDateTime.now().minusDays(2));

            repository.saveAll(Arrays.asList(f1, f2));
            log.info("Test data for flows initialized");
        }
    }
}
