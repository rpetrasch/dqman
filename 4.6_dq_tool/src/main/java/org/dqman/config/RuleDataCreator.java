package org.dqman.config;

import org.dqman.model.DqRule;
import org.dqman.repository.DqRuleRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

/**
 * RuleDataCreator is a class that is used to create test data for rules.
 * It is used to initialize the database with test data.
 */
@Slf4j
@Component
public class RuleDataCreator {

    @Transactional
    public List<DqRule> createRules(DqRuleRepository repository) {
        List<DqRule> rules = null;
        if (repository.count() == 0) {
            DqRule r1 = new DqRule();
            r1.setName("Email Format Check Regex");
            r1.setDescription("Validate email format");
            r1.setRuleType("REGEX");
            r1.setRuleValue("customer.email --> ^[A-Za-z0-9+_.-]+@(.+)$");

            DqRule r2 = new DqRule();
            r2.setName("Email Format Check SQL");
            r2.setDescription("Validate email format");
            r2.setRuleType("SQL");
            r2.setRuleValue("SELECT email FROM customer WHERE email NOT LIKE '%_@_%._%'");

            DqRule r3 = new DqRule();
            r3.setName("Age Range Check");
            r3.setDescription("Ensure age is between 18 and 100");
            r3.setRuleType("SQL");
            r3.setRuleValue("age >= 18 AND age <= 100");

            rules = Arrays.asList(r1, r2, r3);
            repository.saveAll(rules);
            log.info("Test data for rules initialized");
        }
        return rules;
    }
}
