package org.dqman;

class PersonSemanticValidator1 extends PersonBaseVisitor<Boolean> {

    /**
     * Top-level aggregation method.
     * It visits each child 'field' and ensures all of them are valid.
     * The for loop and the visitField method a big if/else if chain is a classic anti-pattern:
     * Solution: Strategy design pattern. (see package validator2)
     */
    @Override
    public Boolean visitPerson(PersonParser.PersonContext ctx) {
        // Iterate through all the fields defined in the grammar.
        for (PersonParser.FieldContext fieldCtx : ctx.field()) {
            // Visit the child field. This will call our visitField method below.
            boolean isFieldValid = visit(fieldCtx);

            // If any field is invalid, the entire person is invalid. Stop and return false.
            if (!isFieldValid) {
                return false;
            }
        }
        // If the loop completes, it means all fields were valid.
        return true;
    }

    /**
     * This method validates a single field. It's called by visitPerson.
     */
    @Override
    public Boolean visitField(PersonParser.FieldContext ctx) {
        // Check if this field is the 'birthdate' field.
        if (ctx.DATE() != null) {
            String dateStr = ctx.DATE().getText();
            String[] parts = dateStr.split("-");
            // NOTE: Add a try-catch block here for production code to handle malformed numbers.
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);

            // Validate month
            if (month < 1 || month > 12) {
                System.err.println("Semantic Error: Invalid month '" + month + "'");
                return false; // This field is invalid.
            }

            // Validate day (simple version)
            if (day < 1 || day > 31) {
                System.err.println("Semantic Error: Invalid day '" + day + "'");
                return false; // This field is invalid.
            }
        }
        else if (ctx.FIRSTNAME() != null) {
            // Check if the firstname is valid
            String firstname = ctx.FIRSTNAME().getText();
            if (firstname.length() < 2) {
                System.err.println("Semantic Error: Invalid firstname '" + firstname + "'");
            }
        }
        else if (ctx.NAME() != null) {
            // ...
        }

        // For all other fields (like name, firstname), we consider them valid by default.
        return true;
    }
}