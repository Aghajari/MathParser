package com.aghajari.math;

import com.aghajari.math.custom.*;
import com.aghajari.math.exception.MathFunctionInvalidArgumentsException;
import com.aghajari.math.exception.MathInvalidParameterException;
import com.aghajari.math.exception.MathParserException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;

public class Functions {

    /**
     * All static methods in {@link Math} and {@link Functions}
     * that returns double and matches arguments will wrap a {@link MathFunction}
     * by {@link MathFunction#wrap(Method)} and import here so
     * the MathParser can recognize the functions as a built-in function.
     */
    private static final List<MathFunction> functions = new ArrayList<>();

    static {
        functions.add(new LogFunction());
        functions.add(new RadicalFunction());
        try {
            functions.add(MathFunction.wrap(Functions.class.getMethod("integral", MathParser.class,
                    String.class, String.class, double.class, double.class), "∫"));
            functions.add(MathFunction.wrap(Functions.class.getMethod("integral", MathParser.class,
                    String.class, String.class, double.class, double.class, double.class), "∫"));
            functions.add(MathFunction.wrap(Functions.class.getMethod("radical",
                    double.class), "√"));
            functions.add(MathFunction.wrap(Functions.class.getMethod("sigma", MathParser.class, String.class,
                    String.class, double.class, double.class), "Σ"));
            functions.add(MathFunction.wrap(Functions.class.getMethod("sigma", MathParser.class, String.class,
                    String.class, double.class, double.class, double.class), "Σ"));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        ArrayList<Method> methods = new ArrayList<>();
        methods.addAll(Arrays.asList(Functions.class.getMethods()));
        methods.addAll(Arrays.asList(Math.class.getMethods()));
        addFunctions(methods, functions);
    }

    public static List<MathFunction> getFunctions() {
        return functions;
    }

    static void addFunctions(Collection<Method> methods, List<MathFunction> functions) {
        Math:
        for (Method method : methods) {
            if (method.getReturnType() == double.class) {
                if (method.getParameterCount() != 1 ||
                        (method.getParameterTypes()[0] != Object[].class && method.getParameterTypes()[0] != Double[].class)) {
                    int index = 0;
                    for (Class<?> cls : method.getParameterTypes()) {
                        if (cls != double.class && cls != String.class && !(index == 0 && cls == MathParser.class))
                            continue Math;
                        index++;
                    }
                }
                functions.add(MathFunction.wrap(method));
            }
        }
    }

    static MathFunction getFunction(String src, int index, String name, int count, List<MathFunction> innerFunctions) throws MathFunctionInvalidArgumentsException {
        MathFunction function = null;
        if (innerFunctions != null)
            for (MathFunction func : innerFunctions)
                if (func.compareNames(name)) {
                    function = func;
                    if (func.getParameterCount() == count || func.getParameterCount() == -1)
                        return func;
                }

        for (MathFunction func : functions)
            if (func.compareNames(name)) {
                function = func;
                if (func.getParameterCount() == count || func.getParameterCount() == -1)
                    return func;
            }

        if (function != null)
            throw new MathFunctionInvalidArgumentsException(src, index, function, count);
        return null;
    }

    /* Built-in functions */

    public static double log(double a, double b) {
        return Math.log(a) / Math.log(b);
    }

    public static double ln(double a) {
        return Math.log(a);
    }

    public static double radical(double a) {
        return Math.sqrt(a);
    }

    public static double radical(double a, double b) {
        if (b <= 2)
            return Math.sqrt(a);
        else if (b == 3)
            return Math.cbrt(a);
        else
            return Math.pow(a, 1.0 / b);
    }

    public static double max(Double... a) {
        double out = a[0];
        for (double b : a)
            out = Math.max(out, b);
        return out;
    }

    public static double min(Double... a) {
        double out = a[0];
        for (double b : a)
            out = Math.min(out, b);
        return out;
    }

    public static double sum(Double... a) {
        double out = 0;
        for (double b : a)
            out += b;
        return out;
    }

    public static double average(Double... a) {
        return avg(a);
    }

    public static double avg(Double... a) {
        return sum(a) / a.length;
    }

    public static double sigma(MathParser parser, String variableName, String exp, double from, double to) throws MathParserException {
        return sigma(parser, variableName, exp, from, to, 1.0);
    }

    public static double sigma(MathParser parser, String variableName, String exp, double from, double to, double step) throws MathParserException {
        if (!Utils.isIdentifier(variableName))
            throw new MathInvalidParameterException("sigma(): invalid variable name (" + variableName + ")");
        if (step == 0)
            throw new MathInvalidParameterException("sigma(): step can not be 0");

        MathParser newParser = parser.clone();
        newParser.addVariable(variableName, from, 0);
        MathParser.MathVariable variable = newParser.getVariable(variableName);
        Double[] ans = new Double[1];
        double out = 0;
        if (step < 0) {
            double tmp = from;
            from = to;
            to = tmp;
            step *= -1;
        }

        for (double i = from; i <= to; i += step) {
            ans[0] = i;
            variable.answer = ans;
            out += newParser.parse(exp);
        }
        return out;
    }

    public static double lim(MathParser parser, String variable, String exp) throws MathParserException {
        return limit(parser, variable, exp);
    }

    public static double limit(MathParser parser, String variable, String exp) throws MathParserException {
        MathParser newParser = parser.clone();
        variable = variable.replace("->", "=");
        if (!variable.contains("="))
            throw new MathInvalidParameterException("limit(): invalid variable (" + variable + "), must be something like x->2");

        String[] var = variable.split("=");
        String variableName = var[0];
        if (!Utils.isIdentifier(variableName))
            throw new MathInvalidParameterException("limit(): invalid variable name (" + variableName + ")");

        double a;
        var[1] = Utils.realTrim(var[1]);
        if (var[1].equalsIgnoreCase("+inf") || var[1].equalsIgnoreCase("inf"))
            a = Double.POSITIVE_INFINITY;
        else if (var[1].equalsIgnoreCase("-inf"))
            a = Double.NEGATIVE_INFINITY;
        else
            a = newParser.parse(var[1]);

        //newParser.setRoundEnabled(false);
        newParser.addVariable(variableName, 0, 0);
        return LimitFunction.limit(newParser, newParser.getVariable(variableName), exp, a);
    }

    public static double derivative(MathParser parser, String variableName, String exp, double x) throws MathParserException {
        if (!Utils.isIdentifier(variableName))
            throw new MathInvalidParameterException("derivative(): invalid variable name (" + variableName + ")");

        MathParser newParser = parser.clone();
        newParser.setRoundEnabled(false);
        newParser.addVariable(variableName, 0, 0);
        MathParser.MathVariable variable = newParser.getVariable(variableName);
        return Derivative.getDerivative(new FunctionWrapper(newParser, exp, variable), x);
    }

    public static double intg(MathParser parser, String variableName, String exp, double lowerLimit, double upperLimit) throws MathParserException {
        return integral(parser, variableName, exp, lowerLimit, upperLimit, 20);
    }

    public static double integral(MathParser parser, String variableName, String exp, double lowerLimit, double upperLimit) throws MathParserException {
        return integral(parser, variableName, exp, lowerLimit, upperLimit, 20);
    }

    public static double integral(MathParser parser, String variableName, String exp, double lowerLimit, double upperLimit, double glPoints) throws MathParserException {
        if (!Utils.isIdentifier(variableName))
            throw new MathInvalidParameterException("integral(): invalid variable name (" + variableName + ")");

        MathParser newParser = parser.clone();
        newParser.setRoundEnabled(false);
        newParser.addVariable(variableName, 0, 0);
        MathParser.MathVariable variable = newParser.getVariable(variableName);
        Integration integration = new Integration(new FunctionWrapper(newParser, exp, variable), lowerLimit, upperLimit);
        integration.gaussQuad((int) Math.abs(glPoints));
        return integration.getIntegralSum();
    }

    public static double gcd(Double... x) {
        double result = 0;
        for (double value : x)
            result = gcd(value, result);
        return result;
    }

    private static double gcd(double a, double b) {
        double x = Math.abs(a);
        double y = Math.abs(b);
        while (y != 0) {
            double z = x % y;
            x = y;
            y = z;
        }
        return x;
    }

    public static double factorial(double x) {
        int number = (int) x;
        long result = 1;
        for (int factor = 2; factor <= number; factor++) {
            result *= factor;
        }
        return result;
    }

    public static double mod(double a, double b) {
        return a % b;
    }

    public static double nor(double a, double b) {
        return not(or(a, b));
    }

    public static double not(double a) {
        return ~((int) a);
    }

    public static double or(double a, double b) {
        return (long) a | (long) b;
    }

    public static double and(double a, double b) {
        return (long) a & (long) b;
    }

    public static double xor(double a, double b) {
        return (long) a ^ (long) b;
    }

    public static double shiftLeft(double a, double b) {
        return (int) a << (int) b;
    }

    public static double shiftRight(double a, double b) {
        return (int) a >> (int) b;
    }

    public static double unsignedShiftRight(double a, double b) {
        return (int) a >>> (int) b;
    }

    public static double sign(double a) {
        return Double.compare(a, 0);
    }

    public static double IF(MathParser parser, String condition, String a, String b) throws MathParserException {
        condition = Utils.realTrim(condition);
        Matcher matcher = Utils.splitIf.matcher(condition);
        double ca, cb = 0;
        String type = "!=";

        if (matcher.find()) {
            ca = parser.parse(matcher.group(1).trim());
            cb = parser.parse(matcher.group(3).trim());
            type = matcher.group(2).trim();
        } else
            ca = parser.parse(condition);

        boolean c;
        switch (type) {
            case "==":
            case "=":
                c = ca == cb;
                break;
            case ">=":
                c = ca >= cb;
                break;
            case "<=":
                c = ca <= cb;
                break;
            case ">":
                c = ca > cb;
                break;
            case "<":
                c = ca < cb;
                break;
            case "<>":
            case "!=":
            default:
                c = ca != cb;
                break;
        }
        return parser.parse(c ? a : b);
    }
}
