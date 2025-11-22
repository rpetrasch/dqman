package org.dqman;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class SyntaxErrorListener extends BaseErrorListener {
    private boolean hasErrors = false;
    private final List<String> errorMessages = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        // An error was found!
        this.hasErrors = true;

        String error = String.format("Syntax Error at line %d:%d - %s", line, charPositionInLine, msg);
        errorMessages.add(error);
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }
}