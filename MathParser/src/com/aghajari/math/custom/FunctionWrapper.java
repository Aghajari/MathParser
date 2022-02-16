package com.aghajari.math.custom;

import com.aghajari.math.MathParser;
import com.aghajari.math.exception.MathParserException;

public final class FunctionWrapper {
    final MathParser parser;
    final String exp;
    final MathParser.MathVariable var;
    double cache = Double.NaN, answer, old;

    public FunctionWrapper(MathParser parser, String exp, MathParser.MathVariable var) {
        this.parser = parser;
        this.exp = exp;
        this.var = var;
    }

    public double apply(double a) throws MathParserException {
        if (!Double.isNaN(cache) && cache == old)
            return answer;
        old = a;
        var.updateAnswer(a);
        return answer = parser.parse(exp);
    }
}