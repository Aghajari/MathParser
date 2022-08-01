package com.aghajari;

import com.aghajari.math.MathParser;
import org.junit.Test;

/**
 * test code from README.md
 */
public class MathParserTest {
    @Test
    public void test(){
        MathParser parser = MathParser.create();
        try {
            System.out.println(parser.parse("2 + 2"));                       // 4.0
            System.out.println(parser.parse("5^2 * (2 + 3 * 4) + 5!/4"));    // 380.0

            parser.addExpression("f(x, y) = 2(x + y)");                      // addFunction
            parser.addExpression("x0 = 1 + 2 ^ 2");                          // addVariable
            parser.addExpression("y0 = 2x0");                                // addVariable
            System.out.println(parser.parse("1 + 2f(x0, y0)/3"));            // 21.0

            System.out.println(parser.parse("sin(3pi/2) + tan(45°)"));

            System.out.println(parser.parse("2 ∫(x, (x^3)/(x+1), 5, 10)"));  // 517.121062
            System.out.println(parser.parse("derivative(x, x^3, 2)"));       // 12.0
            System.out.println(parser.parse("lim(x->2, x^(x + 2)) / 2"));    // 8.0
            System.out.println(parser.parse("Σ(i, 2i^2, 1, 5)"));            // 220.0

            System.out.println(parser.parse("5!/4"));                        // 30.0
            System.out.println(parser.parse("(0b100)!"));                    // 4! = 24.0
            System.out.println(parser.parse("log2((0xFF) + 1)"));            // log2(256) = 8.0
            System.out.println(parser.parse("(0o777)"));                     // 511.0

            System.out.println(parser.parse("2 + if(2^5 >= 5!, 1, 0)"));     // 2.0

            parser.addExpression("gcd(x, y) = if(y = 0, x, gcd(y, x % y))"); // GCD Recursive
            System.out.println(parser.parse("gcd(8, 20)"));                  // 4.0

            System.out.println(parser.parse("gcd(8, 20, 100, 150)"));        // 2.0
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
