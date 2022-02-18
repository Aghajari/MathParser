/*
 * Copyright (C) 2022 - Amir Hossein Aghajari
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */


package com.aghajari.math;

import com.aghajari.math.exception.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;

/**
 * A simple but powerful math parser for java.
 * <pre><code>
 *     MathParser parser = new MathParser();                            // start
 *     parser.addExpression("f(x, y) = 2(x + y)");                      // addFunction
 *     parser.addExpression("x0 = 1 + 2 ^ 2");                          // addVariable
 *     parser.addExpression("y0 = 2x0");                                // addVariable
 *     System.out.println(parser.parse("1 + 2f(x0, y0)/3"));            // 21.0
 * </code></pre>
 * <p>
 * Supports all {@link Math} functions:
 * <pre><code>
 *     System.out.println(parser.parse("cos(45°) ^ (2 * sin(pi/2))"));  // 0.5
 * </code></pre>
 * <p>
 * Supports integral, derivative, limit and sigma:
 * <pre><code>
 *     System.out.println(parser.parse("2 ∫(x, (x^3)/(x+1), 5, 10)"));  // 517.121062
 *     System.out.println(parser.parse("derivative(x, x^3, 2)"));       // 12.0
 *     System.out.println(parser.parse("lim(x->2, x^(x + 2)) / 2"));    // 8.0
 *     System.out.println(parser.parse("Σ(i, 2i^2, 1, 5)"));            // 220.0
 * </code></pre>
 * <p>
 * Supports factorial, binary, hexadecimal and octal:
 * <pre><code>
 *     System.out.println(parser.parse("5!/4"));                        // 30.0
 *     System.out.println(parser.parse("(0b100)!"));                    // 4! = 24.0
 *     System.out.println(parser.parse("log2((0xFF) + 1)"));            // log2(256) = 8.0
 *     System.out.println(parser.parse("(0o777)"));                     // 511.0
 * </code></pre>
 * Supports IF conditions:
 * <pre><code>
 *     System.out.println(parser.parse("2 + if(2^5 >= 5!, 1, 0)"));     // 2.0
 *     parser.addExpression("gcd(x, y) = if(y == 0, x, gcd(y, x%y))");  // GCD Recursive
 *     System.out.println(parser.parse("gcd(8, 20)"));                  // 4.0
 * </code></pre>
 * Supports array arguments:
 * <pre><code>
 *     System.out.println(parser.parse("sum(10, 20, 30, 40)"));         // 100.0
 *     System.out.println(parser.parse("gcd(8, 20, 150)"));             // 2.0
 * </code></pre>
 * <p>
 * Let's see how does <code>MathParser</code> work with an example:
 * exp = cos(x) ^ 2 + (1 + x * sin(x)) / 2
 * <pre>
 * - let tmp1 be cos(x) -> exp = tmp1 ^ 2 + (1 + x * sin(x)) / 2
 *  + tmp1 is ready                                                     // tmp1 = cos(x)
 * - let tmp2 be sin(x) -> exp = tmp1 ^ 2 + (1 + x * tmp2) / 2
 *  + tmp2 is ready                                                     // tmp2 = sin(x)
 * - let tmp3 be (1 + x * tmp2) -> exp = tmp1 ^ 2 + tmp3 / 2
 *  + tmp3 = 1 + x * tmp2
 *  + order tmp3 operations -> tmp3 = 1 + (x * tmp2)
 *      - let tmp4 be (x * tmp2) -> tmp3 = 1 + tmp4
 *          + tmp4 is ready                                             // tmp4 = x * tmp2
 *  + tmp3 = 1 + tmp4
 *  + tmp3 is ready                                                     // tmp3 = 1 + tmp4
 * - exp = tmp1 ^ 2 + tmp3 / 2
 *  + order exp operations -> exp = (tmp1 ^ 2) + tmp3 / 2
 *      - let tmp5 be (tmp1 ^ 2) -> exp = tmp5 + tmp3 / 2
 *          + tmp5 is ready                                             // tmp5 = tmp1 ^ 2
 *  + exp = tmp5 + tmp3 / 2
 *  + order exp operations -> exp = tmp5 + (tmp3 / 2)
 *      - let tmp6 be (tmp3 / 2) -> exp = tmp5 + tmp6
 *          + tmp6 is ready                                             // tmp6 = tmp3 / 2
 *  + exp = tmp5 + tmp6
 *  + exp is ready
 * </pre>
 * <p>
 * Here is the list of inner variables after simplification:
 * tmp1 = cos(x)
 * tmp2 = sin(x)
 * tmp4 = x * tmp2
 * tmp3 = 1 + tmp4
 * tmp5 = tmp1 ^ 2
 * tmp6 = tmp3 / 2
 * exp  = tmp5 + tmp6
 * <p>
 * As you can see, all variables contain only a very small part of
 * the original expression and all the operations in variables
 * have the same priority, So makes the calculation very easy.
 * {@link SimpleParser}, In order of the above list, starts calculating
 * the variables separately to reach the exp which is the final answer.
 *
 * @author AmirHossein Aghajari
 * @version 1.0.0
 */
public class MathParser implements Cloneable {

    /**
     * {@link #setRoundEnabled(boolean)}
     */
    private boolean roundEnabled = true;
    private int roundScale = 6;

    private final ArrayList<MathVariable> variables = new ArrayList<>();
    private final ArrayList<MathFunction> functions = new ArrayList<>();
    private final ArrayList<MathVariable> innerVariables = new ArrayList<>();
    private final AtomicInteger tmpGenerator = new AtomicInteger(0);

    /* The order of operations */
    static final char[] order = {'%', '^', '*', '/', '+', '-'};

    /* The priority of operations connected to order[] */
    static final int[] orderPriority = {3, 2, 1, 1, 0, 0};

    /* Special characters will end name of variables or functions */
    static final char[] special = {'%', '^', '*', '/', '+', '-', ',', '(', ')', '!', '=', '<', '>'};

    /* Basic math operations that parser supports */
    static final HashMap<Character, MathOperation> operations = new HashMap<>();

    static {
        operations.put('^', Math::pow);
        operations.put('*', (a, b) -> a * b);
        operations.put('/', (a, b) -> a / b);
        operations.put('+', Double::sum);
        operations.put('-', (a, b) -> a - b);
        operations.put('%', (a, b) -> a % b);
    }

    private MathParser() {
    }

    public static MathParser create() {
        return new MathParser();
    }

    /**
     * Parses and calculates the expression
     *
     * @param expression the expression to parse and calculate
     * @throws MathParserException                   If something went wrong
     * @throws BalancedParenthesesException          If parentheses aren't balanced
     * @throws MathInvalidParameterException         If parameter of the function is invalid
     * @throws MathFunctionInvalidArgumentsException If the number of arguments is unexpected
     * @throws MathFunctionNotFoundException         If couldn't find the function
     * @throws MathVariableNotFoundException         If couldn't find the variable
     */
    public double parse(String expression) throws MathParserException {
        String org = expression;
        validate(expression);
        try {
            initDefaultVariables();
            expression = firstSimplify(expression);
            calculateVariables();

            return round(calculate(expression, org));
        } catch (Exception e) {
            if (e instanceof MathParserException)
                throw e;
            else if (e.getCause() instanceof MathParserException)
                throw (MathParserException) e.getCause();
            else
                throw new MathParserException(org, e.getMessage(), e);
        }
    }

    /**
     * validate syntax
     */
    private void validate(String src) throws MathParserException {
        Utils.validateBalancedParentheses(src);
    }

    /**
     * Simplify syntax for common functions
     */
    private String firstSimplify(String expression) {
        expression = Utils.realTrim(expression);
        expression = fixDegrees(expression);
        expression = fixFactorial(expression);
        expression = fixDoubleType(expression);
        expression = fixBinary(expression);
        expression = fixHexadecimal(expression);
        expression = fixOctal(expression);
        return expression;
    }

    /**
     * Makes degrees readable for Math trigonometry functions
     * x° => toRadians(x)
     */
    private String fixDegrees(String src) {
        char deg = '°';
        // 24deg | 24degrees => 24°
        // 24rad | 24 radian | 24radians => 24
        if (getVariable("degrees") == null)
            src = src.replaceAll("(?<=\\d)degrees(?=[^\\w]|$)", String.valueOf(deg));
        if (getVariable("deg") == null)
            src = src.replaceAll("(?<=\\d)deg(?=[^\\w]|$)", String.valueOf(deg));
        if (getVariable("radians") == null)
            src = src.replaceAll("(?<=\\d)radians(?=[^\\w]|$)", "");
        if (getVariable("radian") == null)
            src = src.replaceAll("(?<=\\d)radian(?=[^\\w]|$)", "");
        if (getVariable("rad") == null)
            src = src.replaceAll("(?<=\\d)rad(?=[^\\w]|$)", "");

        return fix(src, "toRadians", deg);
    }

    /**
     * Makes factorial readable
     * x! => factorial(x)
     */
    private String fixFactorial(String src) {
        return fix(src, "factorial", '!');
    }

    private String fix(String src, String function, char c) {
        int index;
        while ((index = src.indexOf(c)) != -1) {
            boolean applyToFirst = true, ph = false;
            int count = 0;

            for (int i = index - 1; i >= 0; i--) {
                if (i == index - 1 && src.charAt(i) == ')') {
                    ph = true;
                    count++;
                    continue;
                }
                if (ph) {
                    if (src.charAt(i) == ')') {
                        count++;
                    } else if (src.charAt(i) == '(') {
                        count--;
                    }
                    if (count != 0)
                        continue;
                    ph = false;
                }
                if (!isSpecialSign(src.charAt(i)))
                    continue;

                String sign = isSpecialSign(src.charAt(i)) ? "" : "*";
                i++;
                src = src.substring(0, i) + sign + function + "(" + src.substring(i, index) + ")" + src.substring(index + 1);
                applyToFirst = false;
                break;
            }
            if (applyToFirst)
                src = function + "(" + src.substring(0, index) + ")" + src.substring(index + 1);
        }
        return src;
    }

    /**
     * (2e+2) -> (200.0)
     */
    private String fixDoubleType(String src) {
        Matcher matcher = Utils.doubleType.matcher(src);
        if (matcher.find()) {
            String a = matcher.group(1);
            String b = a;
            String e = matcher.group(2);
            boolean ignoreE = a.endsWith("-") || a.endsWith("+");
            if (!ignoreE) {
                try {
                    b = String.valueOf(Double.parseDouble(a));
                } catch (Exception ignore) {
                    ignoreE = true;
                }
            }
            if (ignoreE) {
                b = a.substring(0, a.indexOf(e)) + " " + a.substring(a.indexOf(e));
            }
            src = src.substring(0, matcher.start() + 1) + b + src.substring(matcher.end() - 1);
            return fixDoubleType(src.trim());
        }
        return src.trim();
    }

    /**
     * (0b010) -> (2)
     */
    private String fixBinary(String src) {
        Matcher matcher = Utils.binary.matcher(src);
        if (matcher.find()) {
            String a = matcher.group(0);
            long value = Long.parseLong(a.substring(3, a.length() - 1), 2);
            src = src.substring(0, matcher.start() + 1) + value + src.substring(matcher.end() - 1);
            return fixBinary(src);
        }
        return src;
    }

    /**
     * (0x0FF) -> (255)
     */
    private String fixHexadecimal(String src) {
        Matcher matcher = Utils.hexadecimal.matcher(src);
        if (matcher.find()) {
            String a = matcher.group(0);
            long value = Long.parseLong(a.substring(3, a.length() - 1), 16);
            src = src.substring(0, matcher.start() + 1) + value + src.substring(matcher.end() - 1);
            return fixHexadecimal(src);
        }
        return src;
    }

    /**
     * (0o027) -> (23)
     */
    private String fixOctal(String src) {
        Matcher matcher = Utils.octal.matcher(src);
        if (matcher.find()) {
            String a = matcher.group(0);
            long value = Long.parseLong(a.substring(3, a.length() - 1), 8);
            src = src.substring(0, matcher.start() + 1) + value + src.substring(matcher.end() - 1);
            return fixOctal(src);
        }
        return src;
    }

    /**
     * Adds default constants {pi, e}
     */
    private void initDefaultVariables() {
        addConst("e", Math.E);
        addConst("Π", Math.PI);
        addConst("π", Math.PI);
        addConst("pi", Math.PI);
    }

    /**
     * Calculate the answer of variables
     */
    private void calculateVariables() throws MathParserException {
        for (MathVariable variable : variables) {
            if (!variable.hasFound) {
                validate(variable.expression);
                variable.answer = new Double[]{round(calculate(fixDegrees(variable.expression), variable.original))};
                variable.hasFound = true;
                //System.out.println(variable.name + " = " + variable.getAnswer());
            }
        }
    }

    /**
     * Rounds the value if {@link #roundEnabled} is true.
     *
     * @see #setRoundEnabled(boolean)
     * @see #isRoundEnabled()
     */
    private double round(double a) {
        if (!roundEnabled || Double.isInfinite(a) || Double.isNaN(a))
            return a;
        return BigDecimal.valueOf(a).setScale(roundScale, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Calculates and returns the value of an expression
     *
     * @param exp  the simplified expression
     * @param main the original expression
     */
    private double calculate(String exp, String main) throws MathParserException {
        return calculate(exp, main, false);
    }

    /**
     * Calculates and returns the value of an expression.
     * <p>
     * This function is not going to check the priorities of {@link #operations},
     * but tries to simplify the expression as much as possible and calculate
     * the parentheses from the innermost order and put them in {@link #innerVariables},
     * If the parentheses are related to function calls, adds a {@link MathVariable#function}
     * to the temp variable to identify the function. So eventually a simple expression without
     * parentheses and functions will be created.
     * <p>
     * For example:
     * 2 + (2x + abs(x)) / 2
     * <p>
     * - let tmp1 be abs(x) -> 2 + (2x + tmp1) / 2
     * - let tmp2 be (2x + tmp1) -> 2 + tmp2 / 2
     * - calls {@link #orderAgain} to order operations
     *
     * @param src          the simplified expression
     * @param main         the user-entered expression
     * @param fromExpValue {@link ExpValue} is an expression that contained an unknown variable in
     *                     the first step that was calculated. If fromExpValue is true, it means that
     *                     it is trying to calculate it again in the hope that the unknown variable is
     *                     found. If it is found, returns the final answer, otherwise will throw
     *                     {@link MathVariableNotFoundException}. Basically, functions that will add a
     *                     dynamic variable, such as sigma and integral, need this property to update
     *                     and apply the variable in the second step.
     */
    private double calculate(String src, String main, boolean fromExpValue) throws MathParserException {
        src = Utils.realTrim(src);
        if (src.startsWith("(") && src.endsWith(")") && !src.substring(1).contains("("))
            src = src.substring(1, src.length() - 1);

        while (src.contains("(") || src.contains(")")) {
            Matcher matcher = Utils.innermostParentheses.matcher(src);
            if (matcher.find()) {
                String name = generateTmpName(), exp = matcher.group(0).trim();
                exp = exp.substring(1, exp.length() - 1);
                MathVariable var;

                Matcher matcher2 = Utils.splitParameters.matcher(exp);
                ArrayList<String> answers = new ArrayList<>();
                int startParameter = 0;
                while (matcher2.find()) {
                    answers.add(exp.substring(startParameter, startParameter = matcher2.end() - 1));
                    startParameter++;
                }
                if (answers.isEmpty())
                    answers.add(exp);
                else
                    answers.add(exp.substring(startParameter));

                MathFunction function = null;
                int start = matcher.start();
                String signBefore = (start == 0 || isSpecialSign(Utils.findCharBefore(src, matcher.start()))) ? "" : "*";
                String signAfter = (matcher.end() == src.length() || isSpecialSign(Utils.findCharAfter(src, matcher.end()))) ? "" : "*";

                if (start > 0) {
                    String before = src.substring(0, start);
                    String wordBefore = before.substring(Utils.findBestIndex(before, true)).trim();
                    while (wordBefore.length() > 0 && Character.isDigit(wordBefore.charAt(0)))
                        wordBefore = wordBefore.substring(1);

                    if (wordBefore.length() > 0) {
                        function = Functions.getFunction(main, main.indexOf(wordBefore),
                                wordBefore, answers.size(), functions);
                        if (function != null) {
                            signBefore = "";
                            start -= wordBefore.length();
                        } else if (answers.size() > 1)
                            throw new MathFunctionNotFoundException(main, main.indexOf(wordBefore), wordBefore);
                    }
                }
                if (answers.size() > 1 && function == null)
                    throw new MathFunctionNotFoundException(main, -1, null, matcher.group(0).trim());

                List<Object> answers2 = new ArrayList<>();
                if (function == null) {
                    if (!fromExpValue)
                        try {
                            answers2.add(calculate(answers.get(0), main));
                        } catch (Exception e) {
                            answers2.add(new ExpValue(answers.get(0), main));
                        }
                    else
                        answers2.add(calculate(answers.get(0), main));
                } else {
                    for (int i = 0; i < answers.size(); i++) {
                        if (function.isSpecialParameter(i))
                            answers2.add(Utils.realTrim(answers.get(i)));
                        else {
                            if (!fromExpValue)
                                try {
                                    answers2.add(calculate(answers.get(i), main));
                                } catch (Exception e) {
                                    answers2.add(new ExpValue(answers.get(i), main));
                                }
                            else
                                answers2.add(calculate(answers.get(i), main));
                        }
                    }
                }
                var = new MathVariable(name, answers2, exp, main, function);
                if (function != null)
                    function.attachToParser(this);
                innerVariables.add(var);

                src = src.substring(0, start) + signBefore + name + signAfter + src.substring(matcher.end());
            } else
                break;
        }

        return orderAgain(src, main);
    }

    /**
     * {@link #calculate(String, String, boolean)} has simplified the expression
     * as much as it's possible, It is time to set priorities of {@link #operations}.
     * This function first checks if all operations are in the same priority.
     * If yes, the phrase is the simplest case possible and the final calculations
     * should be done by the {@link SimpleParser}. If not, it parentheses the higher
     * priority operation and sends it back to the {@link #calculate}
     * for simplification. This is done so that in the end a linear expression remains which
     * all operations have the same priority.
     * <p>
     * For example:
     * 2 + tmp2 / 2
     * <p>
     * <pre>
     * + parentheses the division operation -> 2 + (tmp2 / 2)
     * + sends it back to {@link #calculate}
     *  - let tmp3 be (tmp2 / 2) -> 2 + tmp3
     * + final calculation by {@link SimpleParser}
     * </pre>
     * <p>
     * This cycle will also apply to all variables
     */
    private double orderAgain(String src, String main) throws MathParserException {
        boolean allInSamePriority = true;
        int highestPriority = -1;
        for (int i = 0; i < order.length; i++) {
            if (src.contains(String.valueOf(order[i]))) {
                if (highestPriority != -1 && orderPriority[i] != highestPriority) {
                    allInSamePriority = false;
                    break; // the first ones always have higher priority
                }
                highestPriority = Math.max(highestPriority, orderPriority[i]);
            }
        }

        if (!allInSamePriority) {
            int ind = -1;
            char op = '+';
            for (int i = 0; i < order.length; i++) {
                if (orderPriority[i] == highestPriority) {
                    int ind2 = src.indexOf(order[i]);
                    if (ind2 != -1 && (ind == -1 || ind > ind2)) {
                        ind = ind2;
                        op = order[i];
                    }
                }
            }

            if (ind != -1) {
                int index;
                String wordAfter = src.substring(ind + 1, ind + 1 + Utils.findBestIndex(src.substring(ind + 1), false));
                String wordBefore = src.substring(index = Utils.findBestIndex(src.substring(0, ind), true), ind);

                src = src.substring(0, index) + "(" + wordBefore + op + wordAfter + ")"
                        + src.substring(ind + wordAfter.length() + 1);
            }
        }

        if (src.contains("(") || src.contains(")"))
            return calculate(src, main);

        return new SimpleParser(src, main).parse();
    }

    /**
     * Generates a new unique name for temp variables in format of __tmp{index}
     */
    private String generateTmpName() {
        String name;
        do {
            name = "__tmp" + tmpGenerator.incrementAndGet();
        } while (getVariable(name) != null);
        return name;
    }

    /**
     * Adds support of an expression to this {@link MathParser},
     * the expression must contains {@code =},
     * There are two types of phrases. Variable and Function
     * Variables are in the form of {@code name = expression}
     * Functions are in the form of {@code name(x, y, ..) = expression}
     * <p>
     * For example:
     * <pre><code>
     *      MathParser parser = MathParser.create();
     *      parser.addExpression("f(x, y) = x ^ y");
     *      parser.addExpression("x0 = 2");
     *      parser.addExpression("y0 = 4");
     *      System.out.println(parser.parse("f(x0, y0)")); // 2^4 = 16.0
     * </code></pre>
     */
    public void addExpression(String exp) {
        String[] var = {exp.substring(0, exp.indexOf('=')), exp.substring(exp.indexOf('=') + 1)};
        if (var[0].contains("("))
            addFunction(MathFunction.wrap(exp));
        else
            addVariable(var[0].trim(), var[1].trim());
    }

    /**
     * Adds a new variable to this {@link MathParser}
     *
     * @param name       variable name
     * @param expression expression of variable
     * @see #addVariable(String, double)
     * @see #addExpression(String)
     */
    public void addVariable(String name, String expression) {
        removeVariable(name);
        variables.add(new MathVariable(name, expression));
    }

    /**
     * Adds a new variable at specific index to this {@link MathParser}
     *
     * @param name       variable name
     * @param expression expression of variable
     * @param index      index of variable in {@link #getVariables()} list
     * @see #addVariable(String, double, int)
     * @see #addExpression(String)
     */
    public void addVariable(String name, String expression, int index) {
        removeVariable(name);
        variables.add(index, new MathVariable(name, expression));
    }

    /**
     * Adds a new variable to this {@link MathParser}
     *
     * @param name  variable name
     * @param value value of variable
     * @see #addVariable(String, String)
     */
    public void addVariable(String name, double value) {
        removeVariable(name);
        variables.add(new MathVariable(name, value));
    }

    /**
     * Adds a new variable at specific index to this {@link MathParser}
     *
     * @param name  variable name
     * @param value value of variable
     * @param index index of variable in {@link #getVariables()} list
     * @see #addVariable(String, String, int)
     */
    public void addVariable(String name, double value, int index) {
        removeVariable(name);
        variables.add(index, new MathVariable(name, value));
    }

    /**
     * Adds a new function to this {@link MathParser}
     *
     * @see #addExpression(String)
     */
    public void addFunction(MathFunction function) {
        functions.add(function);
    }

    /**
     * Adds set of functions to this {@link MathParser}
     * based on {@link Method}
     * Only the methods that return <code>double</code> are supported.
     * The method can get {@link MathParser} only as the first argument.
     * The method can get String only if {@link MathFunction#isSpecialParameter(int)}
     * returns true for the index of argument.
     * The method can get array of double in form of <code>Object...</code>
     * The method must be static and have at least one argument
     *
     * @see #addExpression(String)
     */
    public void addFunctions(Collection<Method> methods) {
        Functions.addFunctions(methods, functions);
    }

    /**
     * @see #addFunctions(Collection)
     */
    public void addFunctions(Class<?> cls) {
        Functions.addFunctions(Arrays.asList(cls.getMethods()), functions);
    }

    /**
     * Removes the specified variable from {@link #getVariables()}
     */
    public void removeVariable(String name) {
        name = name.trim().toLowerCase();
        for (MathVariable variable : variables)
            if (variable.name.equals(name)) {
                variables.remove(variable);
                return;
            }
    }

    /**
     * Clears all imported variables
     */
    public void clearVariables() {
        variables.clear();
    }

    /**
     * Clears all imported functions
     */
    public void clearFunctions() {
        functions.clear();
    }

    /**
     * Returns {@code true} if {@link #getVariables()} contains the specified variable.
     */
    public boolean containsVariable(String name) {
        name = name.trim().toLowerCase();
        for (MathVariable variable : variables)
            if (variable.name.equals(name))
                return true;
        return false;
    }

    /**
     * Returns the list of imported variables
     */
    public ArrayList<MathVariable> getVariables() {
        return variables;
    }

    /**
     * Returns the list of imported functions
     */
    public ArrayList<MathFunction> getFunctions() {
        return functions;
    }

    /**
     * Returns the list of const and temp variables
     */
    ArrayList<MathVariable> getInnerVariables() {
        return innerVariables;
    }

    /**
     * Adds a const variable to {@link #innerVariables};
     */
    private void addConst(String name, double value) {
        removeVariable(name);
        innerVariables.add(new MathVariable(name, value));
    }

    /**
     * Returns the instance of {@link MathVariable} if the variable exists
     * on {@link #getVariables()} or {@link #getInnerVariables()}, Null otherwise.
     */
    MathVariable getVariable(String name) {
        name = name.trim().toLowerCase();
        for (MathVariable variable : variables)
            if (variable.name.equals(name))
                return variable;

        for (MathVariable variable : innerVariables)
            if (variable.name.equals(name))
                return variable;
        return null;
    }

    /**
     * Because of the way floating-point numbers are stored as binary,
     * it is better to round them a bit
     * <p>
     * Some functions like {@link Functions#derivative(MathParser, String, String, double)},
     * won't work with rounded value so you have to disable rounding option for this function calls.
     * (The function itself will do this)
     * <p>
     * The round option is enabled by default
     */
    public void setRoundEnabled(boolean roundEnabled) {
        this.roundEnabled = roundEnabled;
    }

    /**
     * Checks whether the rounding option is enabled for this {@link MathParser} or not
     *
     * @return True if rounding option is enabled, False otherwise
     */
    public boolean isRoundEnabled() {
        return roundEnabled;
    }

    /**
     * Sets scale of decimal
     *
     * @see #setRoundEnabled(boolean)
     * @see #getRoundScale()
     */
    public void setRoundScale(int roundScale) {
        this.roundScale = roundScale;
    }

    /**
     * @return the scale of decimal
     * @see #setRoundScale(int)
     * @see #setRoundEnabled(boolean)
     */
    public int getRoundScale() {
        return roundScale;
    }

    /**
     * @return True if src exists on supported operations, False otherwise
     */
    static boolean isMathSign(String src) {
        src = src.trim();
        if (src.isEmpty())
            return false;
        return isMathSign(src.charAt(0));
    }

    /**
     * @return True if src exists on supported operations, False otherwise
     */
    static boolean isMathSign(char src) {
        return operations.containsKey(src);
    }

    /**
     * @return True if src is an special characters, False otherwise
     */
    private static boolean isSpecialSign(char src) {
        for (char c : special)
            if (c == src)
                return true;
        return false;
    }

    /**
     * Creates a new copy of this {@link MathParser}
     *
     * @return a shallow copy of this {@link MathParser}
     * @noinspection MethodDoesntCallSuperMethod
     */
    public MathParser clone() {
        MathParser newParser = MathParser.create();
        newParser.getVariables().addAll(variables);
        newParser.getInnerVariables().addAll(innerVariables);
        newParser.getFunctions().addAll(functions);
        newParser.tmpGenerator.set(tmpGenerator.get());
        newParser.roundEnabled = roundEnabled;
        newParser.roundScale = roundScale;
        return newParser;
    }

    /**
     * Resets temp variables
     *
     * @param deep True to clear all imported functions and variables, False otherwise
     */
    public void reset(boolean deep) {
        if (deep) {
            variables.clear();
            functions.clear();
        }
        innerVariables.clear();
        tmpGenerator.set(0);
    }

    /**
     * @see #operations
     */
    private interface MathOperation {
        double apply(double a, double b);
    }

    public static class MathVariable {
        final String name, expression, original;
        Object[] answer = {0.0};
        boolean hasFound = false;
        MathFunction function = null;

        MathVariable(String name, String expression) {
            this.name = name.trim().toLowerCase();
            this.expression = expression.trim().toLowerCase();
            this.original = this.name + " = " + this.expression;
        }

        MathVariable(String name, double value) {
            this.name = name.trim().toLowerCase();
            this.expression = String.valueOf(value);
            this.original = this.name + " = " + this.expression;
            this.answer = new Double[]{value};
            this.hasFound = true;
        }

        MathVariable(String name, List<Object> value, String expression, String original, MathFunction function) {
            this.name = name.trim().toLowerCase();
            this.expression = expression;
            this.original = original;
            this.answer = value.toArray();
            this.hasFound = true;
            this.function = function;
        }

        public double getAnswer() throws MathParserException {
            if (function == null)
                return (double) answer[0];
            else {
                return function.calculate(answer);
            }
        }

        public void updateAnswer(Object... answers) {
            this.answer = answers;
        }

        public double getAnswer(MathParser parser) throws MathParserException {
            if (function == null)
                return answer[0] instanceof ExpValue ? ((ExpValue) answer[0]).calculate(parser) : (double) answer[0];
            else {
                function.attachToParser(parser);
                boolean allAreDouble = true;
                List<Object> ans = new ArrayList<>();
                for (Object o : answer) {
                    if (o instanceof ExpValue)
                        ans.add(((ExpValue) o).calculate(parser));
                    else {
                        ans.add(o);
                        if (!(o instanceof Double))
                            allAreDouble = false;
                    }
                }
                if (allAreDouble)
                    //noinspection SuspiciousToArrayCall,RedundantCast
                    return function.calculate((Object[]) ans.toArray(new Double[0]));
                else
                    return function.calculate(ans.toArray());
            }
        }

        public String getName() {
            return name;
        }

        public String getExpression() {
            return expression;
        }

        public void setFunction(MathFunction function) {
            this.function = function;
        }

        public MathFunction getFunction() {
            return function;
        }

        @Override
        public String toString() {
            return getName() + " = " +
                    (function != null ? function.name() + "(" : "")
                    + getExpression() +
                    (function != null ? ")" : "");
        }
    }

    /**
     * A new ExpValue means parser once tried to calculate {@link ExpValue#expression}
     * but something went wrong! (usually a variable is missed), So
     * Parser tries to store the expression in a {@link ExpValue} and calculate it again
     * when needed by {@link MathParser#calculate} and will set fromExpValue true,
     * if something went wrong again it will throw an exception this time so
     * it won't go on a loop, and if everything was ok, it will use the calculated value.
     * <p>
     * Some functions like integral or sigma will add a dynamic variable during parsing,
     * this type of variables will be unknown for parser at the first time.
     * that's why we use {@link ExpValue}, to make sure there won't be any dynamic variable.
     */
    private static class ExpValue {
        final String expression, original;

        ExpValue(String expression, String original) {
            this.expression = expression;
            this.original = original;
        }

        public double calculate(MathParser parser) throws MathParserException {
            return parser.calculate(expression, original, true);
        }
    }

    /**
     * A simple parser to parse a linear expression,
     * Doesn't need to check priority of operations
     */
    class SimpleParser {
        final String original;
        String src;
        MathOperation currentOperation = operations.get('+');
        Double a = 0.0, b = null;

        SimpleParser(String src, String original) {
            this.src = src;
            this.original = original;
        }

        double parse() throws MathParserException {
            trim();

            //System.out.println(src);
            if (b != null) {
                a = currentOperation.apply(a, b);
                b = null;
            }

            if (src.isEmpty())
                return a;

            if (isMathSign(src))
                currentOperation = operations.get(get());
            else {
                String word = nextWord();
                try {
                    b = Double.parseDouble(word);
                } catch (Exception e) {
                    b = tryParseWord(word);
                }
            }
            return parse();
        }

        /**
         * so there is a string which isn't a number,
         * this function will try to parse this word and check for variables
         * "xy" => can be two variables (x or y) or just a variable named xy,
         * figures out what would xy mean. (int this case, being one variable
         * is the higher priority)
         */
        private double tryParseWord(String word) throws MathParserException {
            MathVariable variable = getVariable(word);
            if (variable == null) {
                if (Character.isDigit(word.charAt(0))) {
                    StringBuilder numberFirst = new StringBuilder();
                    for (int i = 0; i < word.length(); i++) {
                        char c = word.charAt(i);
                        if (Character.isDigit(c) || c == '.')
                            numberFirst.append(c);
                        else
                            break;
                    }

                    double coefficient = Double.parseDouble(numberFirst.toString());
                    word = word.substring(numberFirst.length());
                    variable = getVariable(word);
                    if (variable == null)
                        return trySplitVariables(word, coefficient);
                    else
                        return coefficient * variable.getAnswer(MathParser.this);
                } else {
                    return trySplitVariables(word, 1.0);
                }
            } else
                return variable.getAnswer(MathParser.this);
        }

        private double trySplitVariables(String word, double coefficient) throws MathParserException {
            StringBuilder name = new StringBuilder();
            int indexOfStart = 0;
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (i + 1 < word.length() && Character.isDigit(word.charAt(i + 1))) {
                    name.append(c);
                    continue;
                }

                MathVariable variable = getVariable(name.toString() + c);
                if (variable == null) {
                    name.append(c);
                } else {
                    indexOfStart = i + 1;
                    name = new StringBuilder();
                    coefficient *= variable.getAnswer(MathParser.this);
                }
            }
            if (name.length() > 0)
                couldNotFindVariables(original.indexOf(word) + 1 + indexOfStart, name.toString());
            return coefficient;
        }

        /**
         * Couldn't find the variable but let's try to guess what could it be,
         * may help to debug the expression
         */
        private void couldNotFindVariables(int index, String nameOut) throws MathParserException {
            double guess = 0;
            String guessName = "";
            for (MathVariable variable : variables) {
                if (!variable.hasFound)
                    continue;

                double sim = Utils.similarity(nameOut, variable.name);
                if (sim > guess) {
                    guessName = variable.name;
                    guess = sim;
                }
            }
            if (guess == 0)
                throw new MathVariableNotFoundException(original, index, nameOut);
            else
                throw new MathVariableNotFoundException(original, index, nameOut, guessName);
        }

        private void trim() {
            src = src.trim();
        }

        private char get() {
            char c = src.charAt(0);
            src = src.substring(1);
            return c;
        }

        private String nextWord() {
            try {
                int index = Utils.findBestIndex(src, false);
                String word = src.substring(0, index).trim();
                src = src.substring(index);
                return word;
            } catch (Exception ignore) {
                return "";
            }
        }
    }
}
