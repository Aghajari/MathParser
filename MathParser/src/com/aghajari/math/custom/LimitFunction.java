package com.aghajari.math.custom;

import com.aghajari.math.MathParser;
import com.aghajari.math.exception.MathParserException;

/**
 * TO-DO: Write a new function for limit.
 */
public final class LimitFunction {

    private LimitFunction() {
    }

    public static double limit(MathParser parser, MathParser.MathVariable var, String exp, double approach) throws MathParserException {
        FunctionWrapper func = new FunctionWrapper(parser, exp, var);
        double below = limitFromBelow(func, approach);
        double above = limitFromAbove(func, approach);
        //System.out.println(below + " : " + above);

        return (below == above) ? below : Double.NaN;
    }

    public static double limitFromBelow(FunctionWrapper function, double approach) throws MathParserException {
        for (double d = approach - 10; d <= approach; d = approach
                - ((approach - d) / 10)) {
            if (function.apply(d) == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY;
            } else if (function.apply(d) == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            } else if (Double.isNaN(function.apply(d))) {
                return function.apply(approach + ((approach - d) * 10));
            } else {
                if (d == approach) {
                    return function.apply(d);
                } else if (approach - d < 0.00000000001)
                    d = approach;

            }
        }
        return Double.NaN;
    }

    public static double limitFromAbove(FunctionWrapper function, double approach) throws MathParserException {
        for (double d = approach + 10; d >= approach; d = approach
                - ((approach - d) / 10)) {
            if (function.apply(d) == Double.POSITIVE_INFINITY) {
                return Double.POSITIVE_INFINITY;
            } else if (function.apply(d) == Double.NEGATIVE_INFINITY) {
                return Double.NEGATIVE_INFINITY;
            } else if (Double.isNaN(function.apply(d))) {
                return function.apply(approach + ((approach - d) * 10));
            } else {
                if (d == approach) {
                    return function.apply(d);
                } else if (d - approach < 0.00000000001)
                    d = approach;

            }
        }
        return Double.NaN;
    }

}