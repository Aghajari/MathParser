# MathParser
**MathParser** is a simple but powerful open-source math tool that parses and evaluates algebraic expressions written in pure java.

This project is designed for University first project of Advanced-Programming at the Department of Computer Engineering, Amirkabir University of Technology.

## Syntax

1. Create an instance of `MathParser`
```java
MathParser parser = MathParser.create();
```

2. Parse an expression
```java
System.out.println(parser.parse("2 + 2"));                       // 4.0
System.out.println(parser.parse("5^2 * (2 + 3 * 4) + 5!/4"));    // 380.0
```

### Add Expression (Function or Variable)
```java
parser.addExpression("f(x, y) = 2(x + y)");                      // addFunction
parser.addExpression("x0 = 1 + 2 ^ 2");                          // addVariable
parser.addExpression("y0 = 2x0");                                // addVariable
System.out.println(parser.parse("1 + 2f(x0, y0)/3"));            // 21.0
```

### Built-in functions
MathParser identifies all static methods in [Math](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html) and [Built-in Functions](https://github.com/Aghajari/MathParser/blob/main/MathParser/src/com/aghajari/math/Functions.java) and functions are case-insensitive.

```java
System.out.println(parser.parse("sin(3pi/2) + tan(45°)"));
```

Built-in Functions includes integral, derivative, limit and sigma
```java
System.out.println(parser.parse("2 ∫(x, (x^3)/(x+1), 5, 10)"));  // 517.121062
System.out.println(parser.parse("derivative(x, x^3, 2)"));       // 12.0
System.out.println(parser.parse("lim(x->2, x^(x + 2)) / 2"));    // 8.0
System.out.println(parser.parse("Σ(i, 2i^2, 1, 5)"));            // 220.0
```

Supports factorial, binary, hexadecimal, octal:
```java
System.out.println(parser.parse("5!/4"));                        // 30.0
System.out.println(parser.parse("(0b100)!"));                    // 4! = 24.0
System.out.println(parser.parse("log2((0xFF) + 1)"));            // log2(256) = 8.0
System.out.println(parser.parse("(0o777)"));                     // 511.0
```

Supports IF conditions:
```java
System.out.println(parser.parse("2 + if(2^5 >= 5!, 1, 0)"));     // 2.0

parser.addExpression("gcd(x, y) = if(y == 0, x, gcd(y, x%y))");  // GCD Recursive
System.out.println(parser.parse("gcd(8, 20)"));                  // 4.0
```
GCD Recursive was only an example, gcd is a built-in function so you don't need to add it as an expression.
```java
System.out.println(parser.parse("gcd(8, 20, 100, 150)"));        // 2.0
```
Some functions such as `sum`, `max`, `min` and `gcd` get array.

### Import new Functions Easy A!

1. Create a class and static methods as your functions:
```java
public class MyFunctions {

    public static double test(double a, double b) {
        return a * b / 2;
    }

    public static double minus(Double... a) {
        double out = a[0];
        for (int i = 1; i < a.length; i++)
            out -= a[i];
        return out;
    }
    
}
```
2. Add the class to the parser
```java
parser.addFunctions(MyFunctions.class);
```
3. Done!
```java
System.out.println(parser.parse("test(10, 5)"));                 // 25.0
System.out.println(parser.parse("minus(100, 25, 5, 2)"));        // 68.0
```

License
=======

    Copyright 2022 Amir Hossein Aghajari
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


<br><br>
<div align="center">
  <img width="64" alt="LCoders | AmirHosseinAghajari" src="https://user-images.githubusercontent.com/30867537/90538314-a0a79200-e193-11ea-8d90-0a3576e28a18.png">
  <br><a>Amir Hossein Aghajari</a> • <a href="mailto:amirhossein.aghajari.82@gmail.com">Email</a> • <a href="https://github.com/Aghajari">GitHub</a>
</div>
