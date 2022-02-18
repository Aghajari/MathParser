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

import com.aghajari.math.exception.BalancedParenthesesException;

import java.util.regex.Pattern;

public class Utils {

    /* used to check if parentheses are balanced */
    //public final static Pattern balancedParentheses = Pattern.compile("\\((?:[^)(]+|\\((?:[^)(]+|\\([^)(]*\\))*\\))*\\)");
    /* used to find the innermost parentheses */
    public final static Pattern innermostParentheses = Pattern.compile("(\\([^\\(]*?\\))");
    /* used to split function arguments by comma */
    public final static Pattern splitParameters = Pattern.compile(",(?=(?:[^()]*\\([^()]*\\))*[^\\()]*$)");
    /* used to split if condition to two comparable part */
    public final static Pattern splitIf = Pattern.compile("(.*?)(!=|<>|>=|<=|==|>|=|<)(.*?$)");
    /* used to simplify double type values */
    public final static Pattern doubleType = Pattern.compile("\\(([\\d.]+([eE])[\\d+-]+)\\)");
    /* used to simplify binary values */
    public final static Pattern binary = Pattern.compile("\\(0b[01]+\\)");
    /* used to simplify octal values */
    public final static Pattern octal = Pattern.compile("\\(0o[0-7]+\\)");
    /* used to simplify hexadecimal values */
    public final static Pattern hexadecimal = Pattern.compile("\\(0x[0-9a-fA-F]+\\)");

    /**
     * @param src the expression to check
     * @throws BalancedParenthesesException If parentheses aren't balanced
     */
    public static void validateBalancedParentheses(String src) throws BalancedParenthesesException {
        /*String dest = src.replaceAll(balancedParentheses.pattern(), "");
        if (dest.contains(")"))
            throw new BalancedParenthesesException(src, src.indexOf(dest.substring(dest.indexOf(")"))) + 1);
        else if (dest.contains("("))
            throw new BalancedParenthesesException(src, src.indexOf(dest.substring(dest.indexOf("("))) + 1);*/

        if (Utils.realTrim(src).contains("()"))
            throw new BalancedParenthesesException(null, -1);

        int opened = 0;
        for (int i = 0; i < src.length(); ++i)
            if (src.charAt(i) == '(')
                opened++;
            else if (src.charAt(i) == ')') {
                opened--;
                if (opened < 0)
                    throw new BalancedParenthesesException(src, i + 1);
            }

        if (opened != 0)
            throw new BalancedParenthesesException(src, src.length());
    }

    /**
     * @see jdk.internal.joptsimple.internal.Strings#repeat(char, int)
     */
    public static String repeat(char ch, int count) {
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < count; ++i)
            buffer.append(ch);

        return buffer.toString();
    }

    public static char findCharBefore(String src, int start) {
        try {
            src = src.substring(0, start).trim();
            return src.isEmpty() ? '\0' : src.charAt(src.length() - 1);
        } catch (Exception ignore) {
            return '\0';
        }
    }

    public static char findCharAfter(String src, int start) {
        try {
            src = src.substring(start).trim();
            return src.isEmpty() ? '\0' : src.charAt(0);
        } catch (Exception ignore) {
            ignore.printStackTrace();

            return '\0';
        }
    }

    static int findBestIndex(String src, boolean before) {
        int index = before ? src.lastIndexOf(' ') : src.indexOf(' ');
        if (index == -1)
            index = before ? 0 : src.length();
        else if (before)
            index++;

        for (char c : MathParser.special) {
            int id = before ? src.lastIndexOf(c) : src.indexOf(c);
            if (id != -1)
                index = before ? Math.max(index, id + 1) : Math.min(index, id);
        }

        return index;
    }

    /**
     * Calculates the similarity (a number within 0 and 1) between two strings.
     * https://stackoverflow.com/a/16018452/9187189
     */
    public static double similarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0; /* both strings are zero length */
        }
        return (longerLength - getLevenshteinDistance(longer, shorter)) / (double) longerLength;

    }

    /**
     * java.org.apache.commons.lang3.StringUtils#getLevenshteinDistance(CharSequence, CharSequence)
     */
    private static int getLevenshteinDistance(CharSequence s, CharSequence t) {
        int n = s.length();
        int m = t.length();

        if (n == 0) {
            return m;
        }
        if (m == 0) {
            return n;
        }

        if (n > m) {
            // swap the input strings to consume less memory
            final CharSequence tmp = s;
            s = t;
            t = tmp;
            n = m;
            m = t.length();
        }

        final int[] p = new int[n + 1];
        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t
        int upper_left;
        int upper;

        char t_j; // jth character of t
        int cost;

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            upper_left = p[0];
            t_j = t.charAt(j - 1);
            p[0] = j;

            for (i = 1; i <= n; i++) {
                upper = p[i];
                cost = s.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upper_left + cost);
                upper_left = upper;
            }
        }

        return p[n];
    }

    public static boolean isUnsignedInteger(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i)))
                return false;
        }
        return true;
    }

    public static String realTrim(String src) {
        return src.trim().replaceAll("\\s+", "");
    }

    public static boolean isIdentifier(String text) {
        if (text == null || text.isEmpty())
            return false;
        if (!Character.isLetter(text.charAt(0)) && text.charAt(0) != '_')
            return false;
        for (int ix = 1; ix < text.length(); ++ix)
            if (!Character.isLetterOrDigit(text.charAt(ix)) && text.charAt(ix) != '_')
                return false;
        return true;
    }
}
