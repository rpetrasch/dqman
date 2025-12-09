package org.dqman.validator2;

import org.dqman.PersonParser;

public class DateValidator implements FieldValidator {
    @Override
    public boolean isValid(PersonParser.FieldContext ctx) {
        String dateStr = ctx.DATE().getText();
        String[] parts = dateStr.split("-");
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);

        if (month < 1 || month > 12) {
            System.err.println("Semantic Error: Invalid month '" + month + "'");
            return false;
        }
        if (day < 1 || day > 31) { // A more robust check for day is needed for production
            System.err.println("Semantic Error: Invalid day '" + day + "'");
            return false;
        }
        return true;
    }
}