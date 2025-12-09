package org.dqman.config;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import org.dqman.model.DataIntegration;
import org.dqman.model.DqFlow;
import org.dqman.model.DqFlowStep;
import org.dqman.model.DqRule;
import org.dqman.repository.DqFlowRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

/**
 * FlowDataCreator is a class that is used to create test data for flows.
 * It is used to initialize the database with test data.
 */
@Slf4j
@Component
public class FlowDataCreator {

    @Transactional
    public DqFlow createFlows(DqFlowRepository repository, List<DataIntegration> dataIntegrations,
            List<DqRule> rules) {
        if (repository.count() != 0)
            return null;

        DqFlow f1 = new DqFlow();
        f1.setName("Customer Onboarding Flow");
        f1.setDescription("DQ checks for new customers");
        f1.setStatus("PRODUCTION");
        f1.setCreatedDate(ZonedDateTime.now().minusDays(10));
        f1.setModifiedDate(ZonedDateTime.now().minusDays(1));

        DqFlow f2 = new DqFlow();
        f2.setName("Existing Customer DQ Check Flow");
        f2.setDescription("DQ checks for all existing customers");
        f2.setStatus("PRODUCTION");
        f2.setCreatedDate(ZonedDateTime.now().minusDays(10));
        f2.setModifiedDate(ZonedDateTime.now().minusDays(1));

        DqFlowStep s1 = new DqFlowStep();
        s1.setName("Load CRM Data");
        s1.setDescription("Load data from CRM");
        s1.setIsInitial(true);
        s1.setType("DATA SOURCE");
        s1.setIntegration(dataIntegrations.get(1));
        f2.addStep(s1);

        DqFlowStep s2 = new DqFlowStep();
        s2.setName("Validate Emails regex");
        s2.setDescription("Check email format (regex)");
        s2.setIsFinal(true);
        s2.setType("DQ RULE");
        s2.setRule(rules.get(0));
        f2.addStep(s2);

        DqFlowStep s3 = new DqFlowStep();
        s3.setName("Validate Emails SQL");
        s3.setDescription("Check email format (SQL)");
        s3.setIsFinal(false);
        s3.setType("DQ RULE");
        s3.setRule(rules.get(1));
        f2.addStep(s3);

        DqFlowStep s4 = new DqFlowStep();
        s4.setName("Write DQ Results to CSV");
        s4.setDescription("Write DQ results to CSV");
        s4.setIsFinal(true);
        s4.setType("DATA SINK");
        s4.setIntegration(dataIntegrations.get(6));
        f2.addStep(s4);

        // Step wiring
        s1.addSuccessor(s2);
        s1.addSuccessor(s3);
        s2.addPredecessor(s1);
        s3.addPredecessor(s1);
        s3.addSuccessor(s4);
        s4.addPredecessor(s3);

        DqFlow f3 = new DqFlow();
        f3.setName("Daily Transaction Check");
        f3.setDescription("Validate daily transactions");
        f3.setStatus("CREATED");
        f3.setCreatedDate(ZonedDateTime.now().minusDays(2));

        List<DqFlow> flows = repository.saveAll(Arrays.asList(f1, f2, f3));
        log.info("Test data for flows initialized");

        return f2;

    }
}
