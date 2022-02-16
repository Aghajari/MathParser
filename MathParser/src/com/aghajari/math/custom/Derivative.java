package com.aghajari.math.custom;

import com.aghajari.math.exception.MathParserException;

// https://github.com/allusai/calculus-solver/blob/master/Calculus.java
public class Derivative {
    /*
     *These constants can modified to change the accuracy of approximation
     *A smaller epsilon/step size uses more memory but yields a more
     *accurate approximation of the derivative/integral respectively
     */
    private final static double EPSILON = 0.0000001;


    /**
     * Calculates the derivative around a certain point using
     * a numerical approximation.
     *
     * @param x the x-coordinate at which to approximate the derivative
     * @return double  the derivative at the specified point
     */
    public static double getDerivative(FunctionWrapper function, double x) throws MathParserException {
        //The y-coordinates of the points close to the specified x-coordinates
        double yOne = function.apply(x - EPSILON);
        double yTwo = function.apply(x + EPSILON);

        return (yTwo - yOne) / (2 * EPSILON);
    }

}