package com.aghajari.math.exception;

import com.aghajari.math.MathFunction;

public class MathFunctionNotFoundException extends MathParserException {

    private final String function;

    public MathFunctionNotFoundException(String src, int index, String function) {
        super(src, index, function == null ? "couldn't find function" : function + "() not found");
        this.function = function;
    }

    public MathFunctionNotFoundException(String src, int index, String function, String message) {
        super(src, index, "couldn't find function: " + message);
        this.function = function;
    }

    public String getFunction() {
        return function;
    }
}
