package org.dqman.validator2;
import org.dqman.PersonBaseVisitor;
import org.dqman.PersonParser;

import java.util.Map;

public class PersonSemanticValidator2 extends PersonBaseVisitor<Boolean> {

    // A registry to hold our validation strategies
    private static final Map<String, FieldValidator> VALIDATORS = Map.of(
            "birthdate", new DateValidator(),
            "name", new NameValidator(),
            "nationality", new NoOpValidator()  // not necessary because default validator is used
    );

    // A default validator for fields without a specific strategy
    private static final FieldValidator DEFAULT_VALIDATOR = new NoOpValidator();

    @Override
    public Boolean visitPerson(PersonParser.PersonContext ctx) {
        for (PersonParser.FieldContext fieldCtx : ctx.field()) {
            if (!visit(fieldCtx)) { // Visit each field
                return false; // Fail fast if any field is invalid
            }
        }
        return true;
    }

    @Override
    public Boolean visitField(PersonParser.FieldContext ctx) {
        // Get the field name (e.g., "name", "birthdate")
        String fieldName = ctx.getChild(0).getText();

        // Look up the appropriate validator in our map. If not found, use the default validator.
        FieldValidator validator = VALIDATORS.getOrDefault(fieldName, DEFAULT_VALIDATOR);

        // Delegate the validation task to the chosen strategy object.
        return validator.isValid(ctx);
    }
}