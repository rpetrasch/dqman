package org.dqman.validator2;

import org.dqman.PersonParser;

// The contract for all field validation strategies
public interface FieldValidator {
    boolean isValid(PersonParser.FieldContext ctx);
}
