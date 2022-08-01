package com.aghajari.math.custom;

import com.aghajari.math.Functions;
import com.aghajari.math.MathFunction;
import com.aghajari.math.MathParser;
import com.aghajari.math.Utils;

/**
 * A {@link MathFunction} for radical, {@link Math#sqrt)} & {@link Math#cbrt(double)}
 * radical2(x) OR √2(x)
 * radical3(x) OR √3(x)
 * ...
 * radical[BASE](x) OR √[Root](x)
 */
public class RadicalFunction implements MathFunction {
    int root;

    @Override
    public String name() {
        return "radical";
    }

    @Override
    public boolean compareNames(String name) {
        if (name.trim().toLowerCase().startsWith("radical")) {
            name = name.substring(7);
        } else if (name.startsWith("√"))
            name = name.substring("√".length());
        else
            return false;

        if (Utils.isUnsignedInteger(name)) {
            root = Integer.parseInt(name);
            return true;
        }
        return false;
    }

    @Override
    public double calculate(Object... parameters) {
        return Functions.radical((double) parameters[0], root);
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
