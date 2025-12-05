package org.dqman.config;

import org.dqman.model.DqRule;
import org.dqman.repository.DqRuleRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

/**
 * RuleDataCreator is a class that is used to create test data for rules.
 * It is used to initialize the database with test data.
 */
@Slf4j
public class RuleDataCreator {

    public static void createRules(DqRuleRepository repository) {
        if (repository.count() == 0) {
            DqRule r1 = new DqRule();
            r1.setName("Email Format Check");
            r1.setDescription("Validate email format");
            r1.setRuleType("REGEX");
            r1.setRuleValue("^[A-Za-z0-9+_.-]+@(.+)$");
            r1.setTargetTable("customers");
            r1.setTargetColumn("email");

            DqRule r2 = new DqRule();
            r2.setName("Age Range Check");
            r2.setDescription("Ensure age is between 18 and 100");
            r2.setRuleType("SQL");
            r2.setRuleValue("age >= 18 AND age <= 100");
            r2.setTargetTable("customers");
            r2.setTargetColumn("age");

            repository.saveAll(Arrays.asList(r1, r2));
            log.info("Test data for rules initialized");
        }
    }
}
