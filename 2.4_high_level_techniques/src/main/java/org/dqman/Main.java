package org.dqman;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.dqman.validator2.PersonSemanticValidator2;

/**
 * Main class for the Person validation.
 */
public class Main {

    /**
     * Main method to run the Person validation.
     * @param args not used
     */
    public static void main(String[] args) {
        boolean tokenDebug = false;
        // Create a stream from the input string
        CharStream input = CharStreams.fromString("""
                        Person {
                            name: "Schmidt"
                            firstname: "Richard"
                            birthdate: 1990-05-15
                            nationality: "German"
                        }
                        """);

        PersonLexer lexer = new PersonLexer(input);  // Create a lexer
        CommonTokenStream tokens = new CommonTokenStream(lexer); // Get the tokens from the lexer
        if (tokenDebug) {  // Print the tokens to the console
            tokens.fill();
            for (Token token : tokens.getTokens()) {
                System.out.println("Token: " + token.getText() +
                        " Type: " + lexer.getVocabulary().getSymbolicName(token.getType()));
            }
            tokens.seek(0); // Reset for parsing
        }
        PersonParser parser = new PersonParser(tokens); // Create a parser
        parser.removeErrorListeners();  // Remove the default console error listener
        SyntaxErrorListener errorListener = new SyntaxErrorListener();
        parser.addErrorListener(errorListener);  // Add own custom listener

        ParseTree tree = parser.person(); // Parse the input to get the tree
        if (errorListener.hasErrors()) { // Syntax  check
            System.out.println("Syntax errors found:");
            for (String error : errorListener.getErrorMessages()) {
                System.out.println(error);
            }
        } else {  // Syntax OK, so continue with semantic checks
            // Use the custom "Visitor" to walk the tree and add semantic checks
            // The version 1 does not use the strategy pattern
            PersonSemanticValidator1 visitor1 = new PersonSemanticValidator1();
            Boolean isValid = visitor1.visit(tree);
            System.out.println("Visitor 1 - Person is valid: " + isValid);

            // Use a better custom "Visitor" using the strategy pattern
            PersonSemanticValidator2 visitor2 = new PersonSemanticValidator2();
            isValid = visitor2.visit(tree);
            System.out.println("Visitor 2 - Person is valid: " + isValid);
        }
    }

}