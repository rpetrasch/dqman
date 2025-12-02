package org.dqman.service;

import org.dqman.model.DqRule;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class RuleEvaluator {

    public boolean evaluate(DqRule rule, String data) {
        if (rule.getRuleType() == null) {
            return false;
        }

        switch (rule.getRuleType().toUpperCase()) {
            case "REGEX":
                return evaluateRegex(rule.getRuleValue(), data);
            case "MANUAL":
                return Boolean.parseBoolean(rule.getRuleValue());
            case "SQL":
                // Placeholder for SQL evaluation
                // In a real scenario, this would execute a query against the target DB
                return true;
            default:
                return false;
        }
    }

    private boolean evaluateRegex(String regex, String data) {
        if (data == null || regex == null) {
            return false;
        }
        return Pattern.compile(regex).matcher(data).matches();
    }
}
