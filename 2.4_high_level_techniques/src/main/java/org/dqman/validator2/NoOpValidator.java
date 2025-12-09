package org.dqman.validator2;

import org.dqman.PersonParser;

// A default validator that always passes.
public class NoOpValidator implements FieldValidator {
    @Override
    public boolean isValid(PersonParser.FieldContext ctx) {
        return true;
    }
}