package com.aghajari;

import com.aghajari.math.MathParser;
import com.aghajari.math.Utils;
import com.aghajari.math.exception.MathParserException;
import com.aghajari.math.exception.MathVariableNotFoundException;

import java.util.Scanner;

public class Main {


    public static void main(String[] args) throws MathParserException {
        Scanner scanner = new Scanner(System.in);
        MathParser parser = MathParser.create();

        for (; ; ) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty())
                continue;

            if (isExp(line)) {
                parser.addExpression(line);

            } else {
                try {
                    System.out.println("\n" + parser.parse(line));

                } catch (MathParserException e) {
                    if (e instanceof MathVariableNotFoundException) {
                        System.out.println(e.getMessage());
                        continue;
                    }
                    e.printStackTrace();
                    return;
                }
                break;
            }
        }

        if (!parser.getVariables().isEmpty()) {
            System.out.println("Variables:");
            for (MathParser.MathVariable variable : parser.getVariables())
                System.out.println(variable.getName() + " = " + variable.getExpression() + " = " + variable.getAnswer());
        }
    }

    public static boolean isExp(String line) {
        return line.contains("=") &&
                (Utils.isIdentifier(Utils.realTrim(line.substring(0, line.indexOf('='))))
                        || line.substring(0, line.indexOf('=')).contains(","));
    }
}
