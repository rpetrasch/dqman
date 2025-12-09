package org.dqman.validator2;

import org.dqman.PersonParser;

public class NameValidator implements FieldValidator{
    @Override
    public boolean isValid(PersonParser.FieldContext ctx) {
        // Check if the firstname is valid
        String name = ctx.NAME().getText();
        if (name.length() < 2) {
            System.err.println("Semantic Error: Invalid name '" + name + "'");
            return false;
        }
        return true;
    }
}
