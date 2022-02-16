package com.aghajari.math.exception;

public class MathVariableNotFoundException extends MathParserException {

    final String variableName, guess;

    public MathVariableNotFoundException(String src, int index, String variableName) {
        super(src, index, variableName + " not found!");
        this.variableName = variableName;
        guess = null;
    }

    public MathVariableNotFoundException(String src, int index, String variableName, String guess) {
        super(src, index, variableName + " not found, did you mean " + guess + "?");
        this.variableName = variableName;
        this.guess = guess;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getGuess() {
        return guess;
    }
}
