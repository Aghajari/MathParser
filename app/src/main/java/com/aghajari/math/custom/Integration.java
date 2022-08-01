package com.aghajari.math.custom;

import com.aghajari.math.exception.MathParserException;

import java.util.ArrayList;

// https://github.com/gbenroscience/ParserNG/blob/master/src/main/java/math/numericalmethods/Integration.java
public class Integration {

    private FunctionWrapper function = null;    // Function to be integrated
    private boolean setFunction = false;        // = true when Function set
    private double lowerLimit = Double.NaN;     // Lower integration limit
    private double upperLimit = Double.NaN;     // Upper integration limit
    private boolean setLimits = false;          // = true when limits set

    private int glPoints = 0;                   // Number of points in the Gauss-Legendre integration
    private boolean setGLpoints = false;        // = true when glPoints set
    private int nIntervals = 0;                 // Number of intervals in the rectangular rule integrations
    private boolean setIntervals = false;       // = true when nIntervals set

    private double integralSum = 0.0D;          // Sum returned by the numerical integration method
    private boolean setIntegration = false;     // = true when integration performed

    // ArrayLists to hold Gauss-Legendre Coefficients saving repeated calculation
    private static ArrayList<Integer> gaussQuadIndex = new ArrayList<Integer>();         // Gauss-Legendre indices
    private static ArrayList<double[]> gaussQuadDistArrayList = new ArrayList<double[]>();  // Gauss-Legendre distances
    private static ArrayList<double[]> gaussQuadWeightArrayList = new ArrayList<double[]>();// Gauss-Legendre weights

    // Iterative trapezium rule
    private double requiredAccuracy = 0.0D;     // required accuracy at which iterative trapezium is terminated
    private double trapeziumAccuracy = 0.0D;    // actual accuracy at which iterative trapezium is terminated as instance variable
    private static double trapAccuracy = 0.0D;  // actual accuracy at which iterative trapezium is terminated as class variable
    private int maxIntervals = 0;               // maximum number of intervals allowed in iterative trapezium
    private int trapeziumIntervals = 1;         // number of intervals in trapezium at which accuracy was satisfied as instance variable
    private static int trapIntervals = 1;       // number of intervals in trapezium at which accuracy was satisfied as class variable

    // CONSTRUCTORS

    // Constructor taking function to be integrated
    public Integration(FunctionWrapper intFunc) {
        this.function = intFunc;
        this.setFunction = true;
    }

    // Constructor taking function to be integrated and the limits
    public Integration(FunctionWrapper intFunc, double lowerLimit, double upperLimit) {
        this.function = intFunc;
        this.setFunction = true;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.setLimits = true;
    }

    // SET METHODS

    // Set function to be integrated
    public void setFunction(FunctionWrapper intFunc) {
        this.function = intFunc;
        this.setFunction = true;
    }

    // Set limits
    public void setLimits(double lowerLimit, double upperLimit) {
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
        this.setLimits = true;
    }

    // Set lower limit
    public void setLowerLimit(double lowerLimit) {
        this.lowerLimit = lowerLimit;
        if (!Double.isNaN(this.upperLimit)) this.setLimits = true;
    }

    // Set upper limit
    public void setUpperLimit(double upperLimit) {
        this.upperLimit = upperLimit;
        if (!Double.isNaN(this.lowerLimit)) this.setLimits = true;
    }

    // Set number of points in the Gaussian Legendre integration
    public void setGLpoints(int nPoints) {
        this.glPoints = nPoints;
        this.setGLpoints = true;
    }

    // Set number of intervals in trapezoidal, forward or backward rectangular integration
    public void setNintervals(int nIntervals) {
        this.nIntervals = nIntervals;
        this.setIntervals = true;
    }

    // GET METHODS

    // Get the sum returned by the numerical integration
    public double getIntegralSum() {
        if (!this.setIntegration) throw new IllegalArgumentException("No integration has been performed");
        return this.integralSum;
    }

    // GAUSSIAN-LEGENDRE QUADRATURE

    // Numerical integration using n point Gaussian-Legendre quadrature (instance method)
    // All parameters preset
    public double gaussQuad() throws MathParserException {
        if (!this.setGLpoints) throw new IllegalArgumentException("Number of points not set");
        if (!this.setLimits) throw new IllegalArgumentException("One limit or both limits not set");
        if (!this.setFunction) throw new IllegalArgumentException("No integral function has been set");

        double[] gaussQuadDist = new double[glPoints];
        double[] gaussQuadWeight = new double[glPoints];
        double sum = 0.0D;
        double xplus = 0.5D * (upperLimit + lowerLimit);
        double xminus = 0.5D * (upperLimit - lowerLimit);
        double dx = 0.0D;
        boolean test = true;
        int k = -1, kn = -1;

        // Get Gauss-Legendre coefficients, i.e. the weights and scaled distances
        // Check if coefficients have been already calculated on an earlier call
        if (!gaussQuadIndex.isEmpty()) {
            for (k = 0; k < gaussQuadIndex.size(); k++) {
                Integer ki = gaussQuadIndex.get(k);
                if (ki == this.glPoints) {
                    test = false;
                    kn = k;
                }
            }
        }

        if (test) {
            // Calculate and store coefficients
            Integration.gaussQuadCoeff(gaussQuadDist, gaussQuadWeight, glPoints);
            Integration.gaussQuadIndex.add(glPoints);
            Integration.gaussQuadDistArrayList.add(gaussQuadDist);
            Integration.gaussQuadWeightArrayList.add(gaussQuadWeight);
        } else {
            // Recover coefficients
            gaussQuadDist = gaussQuadDistArrayList.get(kn);
            gaussQuadWeight = gaussQuadWeightArrayList.get(kn);
        }

        // Perform summation
        for (int i = 0; i < glPoints; i++) {
            dx = xminus * gaussQuadDist[i];
            sum += gaussQuadWeight[i] * this.function.apply(xplus + dx);
        }
        this.integralSum = sum * xminus;      // rescale
        this.setIntegration = true;         // integration performed
        return this.integralSum;            // return value
    }

    // Numerical integration using n point Gaussian-Legendre quadrature (instance method)
    // All parametes except the number of points in the Gauss-Legendre integration preset
    public double gaussQuad(int glPoints) throws MathParserException {
        this.glPoints = glPoints;
        this.setGLpoints = true;
        return this.gaussQuad();
    }

    // Numerical integration using n point Gaussian-Legendre quadrature (static method)
    // All parametes provided
    public static double gaussQuad(FunctionWrapper intFunc, double lowerLimit, double upperLimit, int glPoints) throws MathParserException {
        Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
        return intgrtn.gaussQuad(glPoints);
    }

    // Returns the distance (gaussQuadDist) and weight coefficients (gaussQuadCoeff)
    // for an n point Gauss-Legendre Quadrature.
    // The Gauss-Legendre distances, gaussQuadDist, are scaled to -1 to 1
    // See Numerical Recipes for details
    public static void gaussQuadCoeff(double[] gaussQuadDist, double[] gaussQuadWeight, int n) {

        double z = 0.0D, z1 = 0.0D;
        double pp = 0.0D, p1 = 0.0D, p2 = 0.0D, p3 = 0.0D;

        double eps = 3e-11;    // set required precision
        double x1 = -1.0D;        // lower limit
        double x2 = 1.0D;        // upper limit

        //  Calculate roots
        // Roots are symmetrical - only half calculated
        int m = (n + 1) / 2;
        double xm = 0.5D * (x2 + x1);
        double xl = 0.5D * (x2 - x1);

        // Loop for  each root
        for (int i = 1; i <= m; i++) {
            // Approximation of ith root
            z = Math.cos(Math.PI * (i - 0.25D) / (n + 0.5D));

            // Refinement on above using Newton's method
            do {
                p1 = 1.0D;
                p2 = 0.0D;

                // Legendre polynomial (p1, evaluated at z, p2 is polynomial of
                //  one order lower) recurrence relationsip
                for (int j = 1; j <= n; j++) {
                    p3 = p2;
                    p2 = p1;
                    p1 = ((2.0D * j - 1.0D) * z * p2 - (j - 1.0D) * p3) / j;
                }
                pp = n * (z * p1 - p2) / (z * z - 1.0D);    // Derivative of p1
                z1 = z;
                z = z1 - p1 / pp;                        // Newton's method
            } while (Math.abs(z - z1) > eps);

            gaussQuadDist[i - 1] = xm - xl * z;            // Scale root to desired interval
            gaussQuadDist[n - i] = xm + xl * z;            // Symmetric counterpart
            gaussQuadWeight[i - 1] = 2.0 * xl / ((1.0 - z * z) * pp * pp);    // Compute weight
            gaussQuadWeight[n - i] = gaussQuadWeight[i - 1];        // Symmetric counterpart
        }
    }

    // TRAPEZIUM METHODS

    // Numerical integration using the trapeziodal rule (instance method)
    // all parameters preset
    public double trapezium() throws MathParserException {
        if (!this.setIntervals) throw new IllegalArgumentException("Number of intervals not set");
        if (!this.setLimits) throw new IllegalArgumentException("One limit or both limits not set");
        if (!this.setFunction) throw new IllegalArgumentException("No integral function has been set");

        double y1 = 0.0D;
        double interval = (this.upperLimit - this.lowerLimit) / this.nIntervals;
        double x0 = this.lowerLimit;
        double x1 = this.lowerLimit + interval;
        double y0 = this.function.apply(x0);
        this.integralSum = 0.0D;

        for (int i = 0; i < nIntervals; i++) {
            // adjust last interval for rounding errors
            if (x1 > this.upperLimit) {
                x1 = this.upperLimit;
                interval -= (x1 - this.upperLimit);
            }

            // perform summation
            y1 = this.function.apply(x1);
            this.integralSum += 0.5D * (y0 + y1) * interval;
            x0 = x1;
            y0 = y1;
            x1 += interval;
        }
        this.setIntegration = true;
        return this.integralSum;
    }

    // Numerical integration using the trapeziodal rule (instance method)
    // all parameters except the number of intervals preset
    public double trapezium(int nIntervals) throws MathParserException {
        this.nIntervals = nIntervals;
        this.setIntervals = true;
        return this.trapezium();
    }

    // Numerical integration using the trapeziodal rule (static method)
    // all parameters to be provided
    public static double trapezium(FunctionWrapper intFunc, double lowerLimit, double upperLimit, int nIntervals) throws MathParserException {
        Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
        return intgrtn.trapezium(nIntervals);
    }

    // Numerical integration using an iteration on the number of intervals in the trapeziodal rule
    // until two successive results differ by less than a predetermined accuracy times the penultimate result
    public double trapezium(double accuracy, int maxIntervals) throws MathParserException {
        this.requiredAccuracy = accuracy;
        this.maxIntervals = maxIntervals;
        this.trapeziumIntervals = 1;

        double summ = trapezium(this.function, this.lowerLimit, this.upperLimit, 1);
        double oldSumm = summ;
        int i = 2;
        for (i = 2; i <= this.maxIntervals; i++) {
            summ = trapezium(this.function, this.lowerLimit, this.upperLimit, i);
            this.trapeziumAccuracy = Math.abs((summ - oldSumm) / oldSumm);
            if (this.trapeziumAccuracy <= this.requiredAccuracy) break;
            oldSumm = summ;
        }

        if (i > this.maxIntervals) {
            System.out.println("accuracy criterion was not met in Integration.trapezium - current sum was returned as result.");
            this.trapeziumIntervals = this.maxIntervals;
        } else {
            this.trapeziumIntervals = i;
        }
        Integration.trapIntervals = this.trapeziumIntervals;
        Integration.trapAccuracy = this.trapeziumAccuracy;
        return summ;
    }

    // Numerical integration using an iteration on the number of intervals in the trapeziodal rule (static method)
    // until two successive results differ by less than a predtermined accuracy times the penultimate result
    // All parameters to be provided
    public static double trapezium(FunctionWrapper intFunc, double lowerLimit, double upperLimit, double accuracy, int maxIntervals) throws MathParserException {
        Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
        return intgrtn.trapezium(accuracy, maxIntervals);
    }

    // Get the number of intervals at which accuracy was last met in trapezium if using the instance trapezium call
    public int getTrapeziumIntervals() {
        return this.trapeziumIntervals;
    }

    // Get the number of intervals at which accuracy was last met in trapezium if using static trapezium call
    public static int getTrapIntervals() {
        return Integration.trapIntervals;
    }

    // Get the actual accuracy acheived when the iterative trapezium calls were terminated, using the instance method
    public double getTrapeziumAccuracy() {
        return this.trapeziumAccuracy;
    }

    // Get the actual accuracy acheived when the iterative trapezium calls were terminated, using the static method
    public static double getTrapAccuracy() {
        return Integration.trapAccuracy;
    }

    // BACKWARD RECTANGULAR METHODS

    // Numerical integration using the backward rectangular rule (instance method)
    // All parameters preset
    public double backward() throws MathParserException {
        if (!this.setIntervals) throw new IllegalArgumentException("Number of intervals not set");
        if (!this.setLimits) throw new IllegalArgumentException("One limit or both limits not set");
        if (!this.setFunction) throw new IllegalArgumentException("No integral function has been set");

        double interval = (this.upperLimit - this.lowerLimit) / this.nIntervals;
        double x = this.lowerLimit + interval;
        double y;
        this.integralSum = 0.0D;

        for (int i = 0; i < this.nIntervals; i++) {
            // adjust last interval for rounding errors
            if (x > this.upperLimit) {
                x = this.upperLimit;
                interval -= (x - this.upperLimit);
            }

            // perform summation
            y = this.function.apply(x);
            this.integralSum += y * interval;
            x += interval;
        }

        this.setIntegration = true;
        return this.integralSum;
    }

    // Numerical integration using the backward rectangular rule (instance method)
    // all parameters except number of intervals preset
    public double backward(int nIntervals) throws MathParserException {
        this.nIntervals = nIntervals;
        this.setIntervals = true;
        return this.backward();
    }

    // Numerical integration using the backward rectangular rule (static method)
    // all parameters must be provided
    public static double backward(FunctionWrapper intFunc, double lowerLimit, double upperLimit, int nIntervals) throws MathParserException {
        Integration intgrtn = new Integration(intFunc, lowerLimit, upperLimit);
        return intgrtn.backward(nIntervals);
    }

    // FORWARD RECTANGULAR METHODS

    // Numerical integration using the forward rectangular rule
    // all parameters preset
    public double forward() throws MathParserException {

        double interval = (this.upperLimit - this.lowerLimit) / this.nIntervals;
        double x = this.lowerLimit;
        double y;
        this.integralSum = 0.0D;

        for (int i = 0; i < this.nIntervals; i++) {
            // adjust last interval for rounding errors
            if (x > this.upperLimit) {
                x = this.upperLimit;
                interval -= (x - this.upperLimit);
            }

            // perform summation
            y = this.function.apply(x);
            this.integralSum += y * interval;
            x += interval;
        }
        this.setIntegration = true;
        return this.integralSum;
    }

    // Numerical integration using the forward rectangular rule
    // all parameters except number of intervals preset
    public double forward(int nIntervals) throws MathParserException {
        this.nIntervals = nIntervals;
        this.setIntervals = true;
        return this.forward();
    }

    // Numerical integration using the forward rectangular rule (static method)
    // all parameters provided
    public static double forward(FunctionWrapper integralFunc, double lowerLimit, double upperLimit, int nIntervals) throws MathParserException {
        Integration intgrtn = new Integration(integralFunc, lowerLimit, upperLimit);
        return intgrtn.forward(nIntervals);
    }


}