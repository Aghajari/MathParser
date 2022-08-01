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

import com.aghajari.math.exception.MathFunctionInvalidArgumentsException;
import com.aghajari.math.exception.MathParserException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public interface MathFunction {

    /**
     * @return the name of function
     */
    String name();

    /**
     * @return True if name mentions this function
     */
    boolean compareNames(String name);

    /**
     * Calculates and returns the value
     * Parameters are usually double values
     */
    double calculate(Object... parameters) throws MathParserException;

    int getParameterCount();

    /**
     * @return True if the parameter at specified index won't be a double value (String otherwise)
     */
    boolean isSpecialParameter(int index);

    /**
     * Calls when this function just attached to a parser or it's parent variable called to calculate
     */
    void attachToParser(MathParser parser);

    static MathFunction wrap(final Method method) {
        return wrap(method, method.getName());
    }

    static MathFunction wrap(final Method method, final String name) {
        return new MathFunction() {

            MathParser parser = null;

            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean compareNames(String name) {
                return name().trim().equalsIgnoreCase(name.trim());
            }

            @Override
            public double calculate(Object... parameters) throws MathParserException {
                try {
                    List<Object> pars = new ArrayList<>();
                    if (method.getParameterTypes()[0] == MathParser.class)
                        pars.add(parser);
                    if (getParameterCount() == -1) {
                        pars.add(parameters);
                    } else {
                        pars.addAll(Arrays.asList(parameters));
                    }
                    return (double) method.invoke(null, pars.toArray());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    if (e.getCause() instanceof MathParserException)
                        throw (MathParserException) e.getCause();
                }
                return (double) parameters[0];
            }

            @Override
            public int getParameterCount() {
                int count = method.getParameterCount();
                int first = 0;
                if (count >= 1 && method.getParameterTypes()[0] == MathParser.class) {
                    count--;
                    first++;
                }
                if (count == 1 && method.getParameterTypes()[first].isArray())
                    return -1;

                return count;
            }

            @Override
            public boolean isSpecialParameter(int index) {
                int first = 0;
                if (method.getParameterCount() >= 1
                        && method.getParameterTypes()[0] == MathParser.class)
                    first++;

                if (method.getParameterCount() <= index + first)
                    return false;

                return method.getParameterTypes()[index + first] == String.class;
            }

            @Override
            public void attachToParser(MathParser parser) {
                this.parser = parser;
            }
        };
    }

    static MathFunction wrap(String exp) {
        exp = Utils.realTrim(exp);
        String[] var = {exp.substring(0, exp.indexOf('=')), exp.substring(exp.indexOf('=') + 1)};
        String name = var[0].substring(0, var[0].indexOf('('));
        String vars = var[0].substring(var[0].indexOf('(') + 1, var[0].indexOf(')'));
        return wrap(name, vars.split(","), var[1]);
    }

    static MathFunction wrap(final String functionName, final String[] variables, final String exp) {
        final String name = functionName.trim();

        return new MathFunction() {

            MathParser parser = null;
            MathParser.MathVariable[] mVariables = null;

            @Override
            public String name() {
                return name;
            }

            @Override
            public boolean compareNames(String name) {
                return name().trim().equalsIgnoreCase(name.trim());
            }

            @Override
            public double calculate(Object... parameters) throws MathParserException {
                if (parameters.length != mVariables.length)
                    throw new MathFunctionInvalidArgumentsException(null, -1, this, parameters.length);

                for (int i = 0; i < parameters.length; i++)
                    mVariables[i].updateAnswer(parameters[i]);

                return parser.parse(exp);
            }

            @Override
            public int getParameterCount() {
                return variables.length;
            }

            @Override
            public boolean isSpecialParameter(int index) {
                return false;
            }

            @Override
            public void attachToParser(MathParser parser) {
                this.parser = parser.clone();
                mVariables = new MathParser.MathVariable[variables.length];
                int i = 0;
                for (String var : variables) {
                    this.parser.addVariable(var.trim(), 0, 0);
                    mVariables[i++] = this.parser.getVariable(var.trim());
                }
            }
        };
    }
}
