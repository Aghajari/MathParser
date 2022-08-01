package com.aghajari.math.custom;

import com.aghajari.math.Functions;
import com.aghajari.math.MathFunction;
import com.aghajari.math.MathParser;
import com.aghajari.math.Utils;


/**
 * A {@link MathFunction} for {@link Math#log(double)}
 * log2(x)
 * log3(x)
 * ...
 * log[BASE](x)
 */
public class LogFunction implements MathFunction {
    int base;

    @Override
    public String name() {
        return "log";
    }

    @Override
    public boolean compareNames(String name) {
        if (name.trim().toLowerCase().startsWith("log")) {
            name = name.substring(3);
            if (Utils.isUnsignedInteger(name)) {
                base = Integer.parseInt(name);
                return true;
            }
        }
        return false;
    }

    @Override
    public double calculate(Object... parameters) {
        return Functions.log((double) parameters[0], base);
    }

    @Override
    public int getParameterCount() {
        return 1;
    }

    @Override
    public boolean isSpecialParameter(int index) {
        return false;
    }

    @Override
    public void attachToParser(MathParser parser) {
    }
}
